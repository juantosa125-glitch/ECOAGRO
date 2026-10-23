-- =============================================================================
-- EcoAgro - Migración para el módulo Admin-Staff y Seguridad
-- Agrega estados de cuenta, auditoría de eventos e incidentes de seguridad
-- =============================================================================

-- 1. Estados de cuenta en la tabla de usuarios
ALTER TABLE user
    ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN status_reason VARCHAR(255) NULL,
    ADD COLUMN status_updated_at TIMESTAMP NULL;

-- 2. Registro de actividades y eventos de seguridad
CREATE TABLE IF NOT EXISTS security_log (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NULL,
    actor_id INT NULL,
    event_type VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (id_user) REFERENCES user(id_user) ON DELETE SET NULL,
    CONSTRAINT fk_log_actor FOREIGN KEY (actor_id) REFERENCES user(id_user) ON DELETE SET NULL,
    INDEX idx_log_created_at (created_at),
    INDEX idx_log_event_type (event_type),
    INDEX idx_log_user (id_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Gestión de incidentes de seguridad
CREATE TABLE IF NOT EXISTS security_incident (
    id_incident INT AUTO_INCREMENT PRIMARY KEY,
    id_user_affected INT NOT NULL,
    reported_by INT NOT NULL,
    assigned_to INT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution_notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_incident_affected FOREIGN KEY (id_user_affected) REFERENCES user(id_user),
    CONSTRAINT fk_incident_reporter FOREIGN KEY (reported_by) REFERENCES user(id_user),
    CONSTRAINT fk_incident_assigned FOREIGN KEY (assigned_to) REFERENCES user(id_user) ON DELETE SET NULL,
    INDEX idx_incident_status (status),
    INDEX idx_incident_category (category),
    INDEX idx_incident_user (id_user_affected)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Historial y seguimiento de incidentes
CREATE TABLE IF NOT EXISTS incident_history (
    id_history INT AUTO_INCREMENT PRIMARY KEY,
    id_incident INT NOT NULL,
    actor_id INT NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NULL,
    comment TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_incident FOREIGN KEY (id_incident) REFERENCES security_incident(id_incident) ON DELETE CASCADE,
    CONSTRAINT fk_hist_actor FOREIGN KEY (actor_id) REFERENCES user(id_user),
    INDEX idx_hist_incident (id_incident)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
