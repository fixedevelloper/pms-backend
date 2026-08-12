/**
 * Cross-cutting kernel (base entity, error handling, JWT/security plumbing,
 * common web DTOs) shared by every business module. Declared OPEN so its
 * subpackages ({@code exception}, {@code security}, {@code web}, {@code config})
 * remain usable from any module without being treated as a bounded context
 * of its own.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.pms.hotel.shared;
