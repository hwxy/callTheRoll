CREATE TABLE IF NOT EXISTS site_home_visit_daily (
 visit_date DATE NOT NULL,
 pv BIGINT UNSIGNED NOT NULL DEFAULT 0,
 uv BIGINT UNSIGNED NOT NULL DEFAULT 0,
 PRIMARY KEY (visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS site_home_visit_visitor (
 visit_date DATE NOT NULL,
 visitor_hash CHAR(64) NOT NULL,
 PRIMARY KEY (visit_date, visitor_hash),
 KEY idx_site_home_visit_visitor_date (visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
