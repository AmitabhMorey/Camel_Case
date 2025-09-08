package com.securevoting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for audit logging aspects.
 */
@Configuration
@EnableAspectJAutoProxy
public class AuditConfig {
    // AspectJ configuration is handled by the annotation
}