-- =========================================================================
-- Modulo: admin-staff
-- Tablas para gestion de rangos administrativos y asignacion de personal
-- =========================================================================

-- 1. Tabla de roles administrativos
CREATE TABLE IF NOT EXISTS `staff_role` (
  `id_role` INT NOT NULL AUTO_INCREMENT,
  `role_name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id_role`),
  UNIQUE KEY `uk_staff_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Tabla intermedia de asignacion de roles a usuarios
CREATE TABLE IF NOT EXISTS `staff_assignment` (
  `id_assignment` INT NOT NULL AUTO_INCREMENT,
  `id_user` INT NOT NULL,
  `id_role` INT NOT NULL,
  `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_assignment`),
  UNIQUE KEY `uk_staff_assignment_user_role` (`id_user`, `id_role`),
  KEY `idx_staff_assignment_user` (`id_user`),
  KEY `idx_staff_assignment_role` (`id_role`),
  CONSTRAINT `fk_staff_assignment_user` FOREIGN KEY (`id_user`) REFERENCES `user` (`id_user`) ON DELETE CASCADE,
  CONSTRAINT `fk_staff_assignment_role` FOREIGN KEY (`id_role`) REFERENCES `staff_role` (`id_role`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Insercion de los rangos base
INSERT IGNORE INTO `staff_role` (`role_name`, `description`) VALUES
('SUPERADMINISTRADOR', 'Acceso total y administracion global del sistema'),
('ADMIN_SEGURIDAD', 'Administracion de politicas de seguridad, accesos y permisos');
