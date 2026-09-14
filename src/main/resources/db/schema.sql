CREATE TABLE IF NOT EXISTS blueprint (
    author VARCHAR(64) NOT NULL,
    name   VARCHAR(64) NOT NULL,
    CONSTRAINT pk_blueprint PRIMARY KEY (author, name)
);


CREATE TABLE IF NOT EXISTS blueprint_point (
    id      BIGSERIAL PRIMARY KEY,
    author  VARCHAR(64) NOT NULL,
    bp_name VARCHAR(64) NOT NULL,
    ordinal INT NOT NULL,         
    x       INT NOT NULL,
    y       INT NOT NULL,
    CONSTRAINT fk_point_blueprint FOREIGN KEY (author, bp_name)
        REFERENCES blueprint (author, name) ON DELETE CASCADE,
    CONSTRAINT uq_point_ordinal UNIQUE (author, bp_name, ordinal)
);

CREATE INDEX IF NOT EXISTS idx_blueprint_author ON blueprint (author);


INSERT INTO blueprint (author, name) VALUES
    ('john', 'house'),
    ('john', 'garage'),
    ('jane', 'garden')
ON CONFLICT DO NOTHING;

INSERT INTO blueprint_point (author, bp_name, ordinal, x, y) VALUES
    ('john', 'house',  0,  0,  0),
    ('john', 'house',  1, 10,  0),
    ('john', 'house',  2, 10, 10),
    ('john', 'house',  3,  0, 10),
    ('john', 'garage', 0,  5,  5),
    ('john', 'garage', 1, 15,  5),
    ('john', 'garage', 2, 15, 15),
    ('jane', 'garden', 0,  2,  2),
    ('jane', 'garden', 1,  3,  4),
    ('jane', 'garden', 2,  6,  7)
ON CONFLICT DO NOTHING;
