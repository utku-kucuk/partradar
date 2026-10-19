/**
 * Cross-cutting infrastructure shared by all business modules: web error handling, security
 * configuration and common value types. Contains no business logic.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.partradar.shared;

import org.springframework.modulith.ApplicationModule;
