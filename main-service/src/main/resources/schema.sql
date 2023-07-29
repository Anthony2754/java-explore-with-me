CREATE TABLE IF NOT EXISTS users (
   id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   name VARCHAR(250) NOT NULL,
   email VARCHAR(254) NOT NULL,
   CONSTRAINT pk_user PRIMARY KEY (id),
   CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS categories (
   id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   name VARCHAR(50) NOT NULL,
   CONSTRAINT pk_category PRIMARY KEY (id),
   CONSTRAINT uq_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS locations (
   id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   lat FLOAT NOT NULL,
   lon FLOAT NOT NULL,
   CONSTRAINT pk_location PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS events (
   id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   title VARCHAR(120) NOT NULL,
   annotation VARCHAR(2000) NOT NULL,
   category_id BIGINT,
   description VARCHAR(7000) NOT NULL,
   event_date TIMESTAMP WITHOUT TIME ZONE,
   location_id BIGINT,
   paid BOOLEAN DEFAULT FALSE,
   participant_limit INTEGER DEFAULT 0,
   request_moderation BOOLEAN DEFAULT TRUE,
   created_on TIMESTAMP WITHOUT TIME ZONE,
   published_on TIMESTAMP WITHOUT TIME ZONE,
   initiator_id BIGINT,
   state VARCHAR(10) DEFAULT 'PENDING',
   views BIGINT DEFAULT 0,
   CONSTRAINT pk_event PRIMARY KEY (id),
   CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES categories(id),
   CONSTRAINT fk_location FOREIGN KEY (location_id) REFERENCES locations(id),
   CONSTRAINT fk_initiator FOREIGN KEY (initiator_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS compilations (
      id INTEGER GENERATED BY DEFAULT AS IDENTITY NOT NULL,
      pinned BOOLEAN,
      title VARCHAR(50),
      CONSTRAINT COMPILATIONS_PK PRIMARY KEY (id),
      CONSTRAINT uq_compilation_title UNIQUE (title)
);

CREATE TABLE IF NOT EXISTS event_compilation (
   event_id BIGINT NOT NULL CONSTRAINT EVENT_COMPILATION_FK REFERENCES events,
   compilation_id BIGINT NOT NULL CONSTRAINT EVENT_COMPILATION_FK_1 REFERENCES compilations
);

CREATE TABLE IF NOT EXISTS requests (
   id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
   event_id BIGINT,
   requester_id BIGINT,
   status VARCHAR(20),
   created TIMESTAMP WITHOUT TIME ZONE,
   CONSTRAINT pk_request PRIMARY KEY (id),
   CONSTRAINT fk_event FOREIGN KEY (event_id) REFERENCES events(id),
   CONSTRAINT fk_requester FOREIGN KEY (requester_id) REFERENCES users(id)
);





