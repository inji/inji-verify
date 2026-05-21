CREATE TABLE IF NOT EXISTS verify.dcql_query_scope(
    scope character varying(255) NOT NULL,
    dcql_query text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dcql_query_scope_pkey PRIMARY KEY (scope)
);
