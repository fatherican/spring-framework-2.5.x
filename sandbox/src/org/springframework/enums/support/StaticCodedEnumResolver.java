/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.enums.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.enums.CodedEnum;
import org.springframework.rules.Closure;
import org.springframework.rules.Generator;
import org.springframework.rules.closure.ClosureWithoutResult;
import org.springframework.util.Assert;

/**
 * Resolves statically (in java code) defined enumerations.
 * 
 * @author Keith Donald
 */
public class StaticCodedEnumResolver extends AbstractCodedEnumResolver {
    private static final StaticCodedEnumResolver INSTANCE = new StaticCodedEnumResolver();

    public static StaticCodedEnumResolver instance() {
        return INSTANCE;
    }

    /**
     * Call to register all the statically defined enumerations for a specific
     * enumeration <code>Class</code>.
     * <p>
     * Iterates over the static fields of the class and adds all instances of
     * <code>CodedEnum</code> to the list resolvable by this resolver.
     * 
     * @param clazz
     *            The enum class.
     */
    public void registerStaticEnums(final Class clazz) {
        if (logger.isDebugEnabled()) {
            logger
                    .debug("Registering statically defined coded enums for class "
                            + clazz);
        }
        new CodedEnumFieldValueGenerator(clazz).generate(new ClosureWithoutResult() {
            protected void doCallAction(Object value) {
                add((CodedEnum)value);
            }
        });
    }

    /**
     * Generator that generates a list of static field values that can be
     * processed.
     * 
     * @author Keith Donald
     */
    private static class CodedEnumFieldValueGenerator implements Generator {
        private static final Log logger = LogFactory
                .getLog(CodedEnumFieldValueGenerator.class);

        private Class clazz;

        public CodedEnumFieldValueGenerator(Class clazz) {
            Assert.notNull(clazz);
            this.clazz = clazz;
        }

        public void generate(Closure fieldValueCallback) {
            Field[] fields = clazz.getFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if (Modifier.isStatic(field.getModifiers())
                        && Modifier.isPublic(field.getModifiers())) {
                    if (CodedEnum.class.isAssignableFrom(field.getType())) {
                        try {
                            Object value = field.get(null);
                            Assert
                                    .isTrue(CodedEnum.class.isInstance(value),
                                            "Field value must be a CodedEnum instance.");
                            fieldValueCallback.call(value);
                        }
                        catch (IllegalAccessException e) {
                            logger.warn(
                                    "Unable to access field value " + field, e);
                        }
                    }
                }
            }
        }
    }

}