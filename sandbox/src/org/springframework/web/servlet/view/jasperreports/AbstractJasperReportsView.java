/*
 * Created on Sep 16, 2004
 */
package org.springframework.web.servlet.view.jasperreports;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRBshCompiler;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * @author robh
 *  
 */
public abstract class AbstractJasperReportsView extends AbstractUrlBasedView {

	private JasperReport report;

	private Resource reportResource;

	protected final void renderMergedOutputModel(Map model,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		JRDataSource dataSource = locateDataSource(model);

		if (dataSource == null) {
			throw new NoDataSourceException("No Report DataSource Supplied.");
		} 

		response.setContentType(getContentType());
		renderView(report, model, dataSource, response);
	}

	protected void initApplicationContext() {
		super.initApplicationContext();

		String reportPath = getUrl();
		// we know the url is set
		// now try to get the report
		// and then compile it
		reportResource = getApplicationContext().getResource(getUrl());

		try {
			if (reportPath.endsWith(".jasper")) {
				report = JasperManager.loadReport(reportResource
						.getInputStream());
			} else if(reportPath.endsWith(".jrxml")){
				// attempt a compile!
				JRBshCompiler compiler = new JRBshCompiler();
				report = compiler.compileReport(JasperManager
						.loadXmlDesign(reportResource.getInputStream()));
			} else {
				throw new UnrecognizedReportExtensionException("Report URL must end in either .jasper or .jrxml");
			}
		} catch (JRException ex) {
			throw new JasperReportsInitializationException(
					"An exception occured in the Jasper Reports framework during initialization.",
					ex);
		} catch (FileNotFoundException ex) {
			throw new ReportFileNotFoundException(
					"Unable to load report at path: " + getUrl());
		} catch (IOException ex) {
			throw new JasperReportsInitializationException(
					"An exception occured whilst reading report data.", ex);
		}
	}

	/**
	 * Attempts to locate an instance of JRDataSource in a given Map instance.
	 * 
	 * @param model
	 *            The Map to look in.
	 * @return The JRDataSource if found, otherwise null.
	 */
	private JRDataSource locateDataSource(Map model) {
		JRDataSource dataSource = null;

		for (Iterator i = model.values().iterator(); i.hasNext();) {
			Object o = i.next();

			if (o instanceof JRDataSource) {
				dataSource = (JRDataSource) o;
				break;
			}
		}

		return dataSource;
	}

	protected abstract void renderView(JasperReport report, Map model,
			JRDataSource dataSource, HttpServletResponse response)
			throws Exception;

}