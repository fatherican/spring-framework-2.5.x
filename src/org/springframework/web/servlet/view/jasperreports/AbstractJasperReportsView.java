/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view.jasperreports;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRBshCompiler;
import net.sf.jasperreports.engine.design.JRCompiler;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.ui.jasperreports.JasperReportsUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * Base class for all JasperReports views. Applies on-the-fly compilation
 * of report designs as required and coordinates the rendering process.
 * The resource path of the main report needs to be specified as <code>url</code>.
 * <p/>
 * This class is responsible for getting report data from the model that has
 * been provided to the view. The default implementation checks for a model object
 * under the specified <code>reportDataKey</code> first, then falls back to looking
 * for a value of type <code>JRDataSource</code>, <code>java.util.Collection</code>,
 * object array (in that order).
 * <p/>
 * Provides support for sub-reports through the <code>subReportUrls</code> and
 * <code>subReportDataKeys</code> properties.
 * <p/>
 * When using sub-reports, the master report should be configured using the
 * <code>url</code> property and the sub-reports files should be configured using
 * the <code>subReportUrls</code> property. Each entry in the <code>subReportUrls</code>
 * Map corresponds to an individual sub-report. The key of an entry must match up
 * to a sub-report parameter in your report file of type
 * <code>net.sf.jasperreports.engine.JasperReport</code>,
 * and the value of an entry must be the URL for the sub-report file.
 * <p/>
 * For sub-reports that require an instance of <code>JRDataSource</code>, that is,
 * they don't have a hard-coded query for data retrieval, you can include the
 * appropriate data in your model as would with the data source for the parent report.
 * However, you must provide a List of parameter names that need to be converted to
 * <code>JRDataSource</code> instances for the sub-report via the
 * <code>subReportDataKeys</code> property. When using <code>JRDataSource</code>
 * instances for sub-reports, you <i>must</i> specify a value for the
 * <code>reportDataKey</code> property, indicating the data to use for the main report.
 * <p/>
 * Allows for exporter parameters to be configured declatively using the
 * <code>exporterParameters</code> property. This is a <code>Map</code> typed
 * property where the key of an entry corresponds to the fully-qualified name
 * of the static field for the <code>JRExporterParameter</code> and the value
 * of an entry is the value you want to assign to the exporter parameter.
 * <p/>
 * Response headers can be controlled via the <code>headers</code> property. Spring
 * will attempt to set the correct value for the <code>Content-Diposition</code> header
 * so that reports render correctly in Internet Explorer. However, you can override this
 * setting through the <code>headers</code> property.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #setUrl
 * @see #getReportData
 * @see #setSubReportDataKeys(String[])
 * @see #setSubReportUrls(java.util.Properties)
 * @see #setExporterParameters(java.util.Map)
 * @see #setHeaders(java.util.Properties)
 * @since 1.1.3
 */
public abstract class AbstractJasperReportsView extends AbstractUrlBasedView {

	/**
	 * Constant that defines "Content-Disposition" header.
	 */
	private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

	/**
	 * Stores the default Content-Disposition header. Used to make IE play nice.
	 */
	private static final String CONTENT_DISPOSITION_INLINE = "inline";


	/**
	 * A String key used to lookup the <code>JRDataSource</code> in the model.
	 */
	private String reportDataKey;

	/**
	 * Stores the paths to any sub-report files used by this top-level report,
	 * along with the keys they are mapped to in the top-level report file.
	 */
	private Properties subReportUrls;

	/**
	 * Stores the names of any data source objects that need to be converted to
	 * <code>JRDataSource</code> instances and included in the report parameters
	 * to be passed on to a sub-report.
	 */
	private String[] subReportDataKeys;

	/**
	 * The <code>JasperReport</code> that is used to render the view.
	 */
	private JasperReport report;

	/**
	 * Holds mappings between sub-report keys and <code>JasperReport</code> objects.
	 */
	private Map subReports;

	/**
	 * Stores the headers to written with each response
	 */
	private Properties headers;

	/**
	 * Stores the <code>String</code> keyed exporter parameters
	 * passed in by the user. The converted versions of these parameters
	 * are stored in <code>convertedExporterParameters</code>.
	 */
	private Map exporterParameters;

	/**
	 * Store the converted exporter parameters. Conversion involves
	 * changing the key from an instance of <code>String</code> to an
	 * instance of <code>JRExporterParameter</code>.
	 */
	private Map convertedExporterParameters;

	/**
	 * Set the name of the model attribute that represents the report data.
	 * If not specified, the model map will be searched for a matching value type.
	 * <p>A <code>JRDataSource</code> will be taken as-is. For other types, conversion
	 * will apply: By default, a <code>java.util.Collection</code> will be converted
	 * to <code>JRBeanCollectionDataSource</code>, and an object array to
	 * <code>JRBeanArrayDataSource</code>.
	 * <p><b>Note:</b> If you pass in a Collection or object array in the model map
	 * for use as plain report parameter, rather than as report data to extract fields
	 * from, you need to specify the key for the actual report data to use, to avoid
	 * mis-detection of report data by type.
	 *
	 * @see #convertReportData
	 * @see net.sf.jasperreports.engine.JRDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanArrayDataSource
	 */
	public void setReportDataKey(String reportDataKey) {
		this.reportDataKey = reportDataKey;
	}

	/**
	 * Specify resource paths which must be loaded as instances of
	 * <code>JasperReport</code> and passed to the JasperReports engine for
	 * rendering as sub-reports, under the same keys as in this mapping.
	 *
	 * @param subReports mapping between model keys and resource paths
	 * (Spring resource locations)
	 * @see #setUrl
	 * @see org.springframework.context.ApplicationContext#getResource
	 */
	public void setSubReportUrls(Properties subReports) {
		this.subReportUrls = subReports;
	}

	/**
	 * Set the list of names corresponding to the model parameters that will contain
	 * data source objects for use in sub-reports. Spring will convert these objects
	 * to instances of <code>JRDataSource</code> where applicable and will then
	 * include the resulting <code>JRDataSource</code> in the parameters passed into
	 * the JasperReports engine.
	 * <p>The name specified in the list should correspond to an attribute in the
	 * model Map, and to a sub-report data source parameter in your report file.
	 * If you pass in <code>JRDataSource</code> objects as model attributes,
	 * specifing this list of keys is not required.
	 * <p>If you specify a list of sub-report data keys, it is required to also
	 * specify a <code>reportDataKey</code> for the main report, to avoid confusion
	 * between the data source objects for the various reports involved.
	 *
	 * @param subReportDataKeys list of names for sub-report data source objects
	 * @see #setReportDataKey
	 * @see #convertReportData
	 * @see net.sf.jasperreports.engine.JRDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanArrayDataSource
	 */
	public void setSubReportDataKeys(String[] subReportDataKeys) {
		this.subReportDataKeys = subReportDataKeys;
	}

	/**
	 * Specify the set of headers that are included in each of response.
	 *
	 * @param headers the headers to write to each response.
	 */
	public void setHeaders(Properties headers) {
		this.headers = headers;
	}

	/**
	 * Sets the exporter parameters that should be used when rendering a view. The key
	 * of this <code>Map</code> is the fully qualified field name of the <code>JRExporterParameter</code> instance
	 * and the value is the value you wish to assign to the parameter.
	 *
	 * @param parameters the exporter parameters with <code>String</code> keys.
	 */
	public void setExporterParameters(Map parameters) {
		this.exporterParameters = parameters;
	}

	/**
	 * Checks to see that a valid report file URL is supplied in the
	 * configuration. Compiles the report file is necessary.
	 */
	protected void initApplicationContext() throws ApplicationContextException {
		super.initApplicationContext();

		Resource mainReport = getApplicationContext().getResource(getUrl());
		this.report = loadReport(mainReport);

		// Load sub reports if required, and check data source parameters.
		if (this.subReportUrls != null) {
			if (this.subReportDataKeys != null && this.subReportDataKeys.length > 0 &&
					this.reportDataKey == null) {
				throw new ApplicationContextException("'reportDataKey' for main report is required when specifying a value for 'subReportDataKeys'");
			}
			this.subReports = new HashMap(this.subReportUrls.size());
			for (Enumeration urls = this.subReportUrls.propertyNames(); urls.hasMoreElements();) {
				String key = (String) urls.nextElement();
				String path = this.subReportUrls.getProperty(key);
				Resource resource = getApplicationContext().getResource(path);
				this.subReports.put(key, loadReport(resource));
			}
		}

		convertExporterParameters();

		if (this.headers == null) {
			this.headers = new Properties();
		}

		if (!this.headers.containsKey(HEADER_CONTENT_DISPOSITION)) {
			this.headers.setProperty(HEADER_CONTENT_DISPOSITION, CONTENT_DISPOSITION_INLINE);
		}

		initJasperView();
	}

	/**
	 * Converts the exporter parameters configured by the user with <code>String</code> keys
	 * into exporter parameters with <code>JRExporterParameter</code> keys as required by
	 * JasperReports.
	 */
	private void convertExporterParameters() {
		if (exporterParameters != null) {
			convertedExporterParameters = new HashMap();

			for (Iterator itr = exporterParameters.keySet().iterator(); itr.hasNext();) {
				String key = (String) itr.next();
				Object param = exporterParameters.get(key);

				convertedExporterParameters.put(convertToExporterParameter(key), param);
			}
		}
	}

	private JRExporterParameter convertToExporterParameter(String key) {
		int index = key.lastIndexOf('.');
		if (index == -1 || index == key.length()) {
			throw new IllegalArgumentException("Parameter name [" + key + "] is not a valid static field. " +
					"The parameter name must map to a static field such as " +
					"net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI");
		}
		String className = key.substring(0, index);
		String fieldName = key.substring(index + 1);

		try {
			Class cls = ClassUtils.forName(className);
			Field field = cls.getField(fieldName);

			if (JRExporterParameter.class.isAssignableFrom(field.getType())) {
				try {
					return (JRExporterParameter) field.get(null);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalArgumentException("Unable to access field [" + fieldName + "] of class [" + className + "]." +
							" Check that it is static and accessible.");
				}
			}
			else {
				throw new IllegalArgumentException("Field [" + fieldName + "] on class [" + className + "] is not " +
						"assignable from JRExporterParameter - check the type of this field.");
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Class [" + className + "] in key [" + key + "] could not be found.");
		}
		catch (NoSuchFieldException ex) {
			throw new IllegalArgumentException("Field [" + fieldName + "] in key [" + key + "] could not be " +
					"found on class [" + className + "].");
		}
	}

	/**
	 * Called after this class is initialized to allow sub-classes a chance to
	 * perform any additional initialization steps.
	 */
	protected void initJasperView() throws ApplicationContextException {
		;
	}

	/**
	 * Loads a <code>JasperReport</code> from the specified <code>Resource</code>. If
	 * the <code>Resource</code> points to an uncompiled report design file then the
	 * report file is compiled dynamically and loaded into memory.
	 *
	 * @param resource the <code>Resource</code> containing the report definition or design.
	 * @return a <code>JasperReport</code> instance.
	 */
	private JasperReport loadReport(Resource resource) {
		try {
			String fileName = resource.getFilename();
			if (fileName.endsWith(".jasper")) {
				// load pre-compiled report
				if (logger.isInfoEnabled()) {
					logger.info("Loading pre-compiled Jasper Report from " + resource);
				}
				return (JasperReport) JRLoader.loadObject(resource.getInputStream());
			}
			else if (fileName.endsWith(".jrxml")) {
				// compile report on-the-fly
				if (logger.isInfoEnabled()) {
					logger.info("Compiling Jasper Report loaded from " + resource);
				}
				JasperDesign design = JRXmlLoader.load(resource.getInputStream());
				return getReportCompiler().compileReport(design);
			}
			else {
				throw new IllegalArgumentException("Report URL [" + getUrl() + "] must end in either .jasper or .jrxml");
			}
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Could not load JasperReports report for URL [" + getUrl() + "]", ex);
		}
		catch (JRException ex) {
			throw new ApplicationContextException("Could not parse JasperReports report for URL [" + getUrl() + "]", ex);
		}
	}

	/**
	 * Return the JasperReports compiler to use for compiling a ".jrxml"
	 * file into a a report class. Default is <code>JRBshCompiler</code>,
	 * which requires BeanShell on the class path.
	 *
	 * @see net.sf.jasperreports.engine.design.JRCompiler
	 * @see net.sf.jasperreports.engine.design.JRBshCompiler
	 */
	protected JRCompiler getReportCompiler() {
		return new JRBshCompiler();
	}


	/**
	 * Finds the report data to use for rendering the report and then invokes the
	 * <code>renderReport</code> method that should be implemented by the subclass.
	 *
	 * @param model the model map, as passed in for view rendering. Must contain
	 * a report data value that can be converted to a <code>JRDataSource</code>,
	 * acccording to the <code>getReportData</code> method.
	 * @see #getReportData
	 */
	protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		response.setContentType(getContentType());

		// Determine JRDataSource for main report.
		JRDataSource dataSource = getReportData(model);

		if (this.subReports != null) {
			// Expose sub-reports as model attributes.
			model.putAll(this.subReports);

			// Transform any collections etc into JRDataSources for sub reports.
			if (this.subReportDataKeys != null) {
				for (int i = 0; i < this.subReportDataKeys.length; i++) {
					String key = this.subReportDataKeys[i];
					model.put(key, convertReportData(model.get(key)));
				}
			}
		}

		populateHeaders(response);
		renderReport(this.report, model, dataSource, response);
	}

	private void populateHeaders(HttpServletResponse response) {
		// Apply the headers to the response.
		for (Enumeration en = this.headers.propertyNames(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			response.addHeader(key, this.headers.getProperty(key));
		}
	}

	/**
	 * Find an instance of <code>JRDataSource</code> in the given model map or create an
	 * appropriate JRDataSource for passed-in report data.
	 * <p>The default implementation checks for a model object under the
	 * specified "reportDataKey" first, then falls back to looking for a value
	 * of type <code>JRDataSource</code>, <code>java.util.Collection</code>,
	 * object array (in that order).
	 *
	 * @param model the model map, as passed in for view rendering
	 * @return the <code>JRDataSource</code>
	 * @throws IllegalArgumentException if no JRDataSource found
	 * @see #setReportDataKey
	 * @see #convertReportData
	 * @see #getReportDataTypes
	 */
	protected JRDataSource getReportData(Map model) throws IllegalArgumentException {
		// Try model attribute with specified name.
		if (this.reportDataKey != null) {
			Object value = model.get(this.reportDataKey);
			return convertReportData(value);
		}

		// Try to find matching attribute, of given prioritized types.
		Object value = CollectionUtils.findValueOfType(model.values(), getReportDataTypes());
		if (value != null) {
			return convertReportData(value);
		}

		throw new IllegalArgumentException("No report data supplied in model " + model);
	}

	/**
	 * Convert the given report data value to a <code>JRDataSource</code>.
	 * <p>The default implementation delegates to <code>JasperReportUtils</code>.
	 * A <code>JRDataSource</code>, <code>java.util.Collection</code> or object array
	 * is detected. The latter are converted to <code>JRBeanCollectionDataSource</code>
	 * or <code>JRBeanArrayDataSource</code>, respectively.
	 *
	 * @param value the report data value to convert
	 * @return the JRDataSource
	 * @throws IllegalArgumentException if the value could not be converted
	 * @see org.springframework.ui.jasperreports.JasperReportsUtils#convertReportData
	 * @see net.sf.jasperreports.engine.JRDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanArrayDataSource
	 */
	protected JRDataSource convertReportData(Object value) throws IllegalArgumentException {
		return JasperReportsUtils.convertReportData(value);
	}

	/**
	 * Return the value types that can be converted to a JRDataSource,
	 * in prioritized order. Should only return types that the
	 * <code>convertReportData</code> method is actually able to convert.
	 * <p>Default value types are: <code>JRDataSource</code>,
	 * <code>java.util.Collection</code>, object array.
	 *
	 * @return the value types in prioritized order
	 * @see #convertReportData
	 */
	protected Class[] getReportDataTypes() {
		return new Class[]{JRDataSource.class, Collection.class, Object[].class};
	}

	/**
	 * Allows sub-classes to get access to the <code>JasperReport</code> instance
	 * loaded by Spring.
	 *
	 * @return an instance of <code>JasperReport</code>.
	 */
	protected JasperReport getReport() {
		return this.report;
	}

	/**
	 * Allows sub-classes to access the exporter parameters configured by the
	 * user.
	 *
	 * @return a <code>Map</code> containing the exporter parameters with instances of
	 *         <code>JRExporterParameter</code> as the key.
	 */
	protected Map getExporterParameters() {
		return convertedExporterParameters;
	}

	/**
	 * Sub-classes should implement this method to actually render a <code>JasperReport</code>
	 * to the <code>HttpServletResponse</code>.
	 *
	 * @param report the <code>JasperReport</code>
	 * @param model
	 * @param dataSource
	 * @param response
	 */
	protected abstract void renderReport(JasperReport report, Map model, JRDataSource dataSource,
			HttpServletResponse response) throws Exception;


}
