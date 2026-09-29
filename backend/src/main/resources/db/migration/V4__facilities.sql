-- Facilities: buildings < floors < rooms < beds, room types per building, room assets, and the
-- building_managers link (BR-14). Foreign keys have no cascade: the services delete children
-- explicitly, so history rows added by later phases (e.g. registrations.bed_id) also block deletes.

CREATE TABLE buildings (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    code VARCHAR(16)  NOT NULL,
    name VARCHAR(128) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_buildings_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE floors (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    building_id       BIGINT      NOT NULL,
    number            INT         NOT NULL,
    gender_preference VARCHAR(10) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_floors_building_number UNIQUE (building_id, number),
    CONSTRAINT fk_floors_building FOREIGN KEY (building_id) REFERENCES buildings (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE room_types (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    building_id          BIGINT       NOT NULL,
    name                 VARCHAR(64)  NOT NULL,
    capacity             INT          NOT NULL,
    area_m2              DECIMAL(6,2) NOT NULL,
    has_air_conditioning BOOLEAN      NOT NULL,
    has_water_heater     BOOLEAN      NOT NULL,
    bathrooms            INT          NOT NULL,
    monthly_rent         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_room_types_building_name UNIQUE (building_id, name),
    CONSTRAINT fk_room_types_building FOREIGN KEY (building_id) REFERENCES buildings (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Room codes are unique per building; the service checks that, the database per floor.
CREATE TABLE rooms (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    floor_id     BIGINT      NOT NULL,
    room_type_id BIGINT      NOT NULL,
    code         VARCHAR(16) NOT NULL,
    gender       VARCHAR(10) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rooms_floor_code UNIQUE (floor_id, code),
    CONSTRAINT fk_rooms_floor FOREIGN KEY (floor_id) REFERENCES floors (id),
    CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE beds (
    id      BIGINT     NOT NULL AUTO_INCREMENT,
    room_id BIGINT     NOT NULL,
    code    VARCHAR(8) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_beds_room_code UNIQUE (room_id, code),
    CONSTRAINT fk_beds_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE room_assets (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    room_id BIGINT       NOT NULL,
    name    VARCHAR(128) NOT NULL,
    status  VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    KEY ix_room_assets_room (room_id),
    CONSTRAINT fk_room_assets_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE building_managers (
    user_id     BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, building_id),
    KEY ix_building_managers_building (building_id),
    CONSTRAINT fk_building_managers_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_building_managers_building FOREIGN KEY (building_id) REFERENCES buildings (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
