-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script : v0.18.0 to v0.19.0
-- Database       : inji_verify
-- Purpose        : Apply schema changes introduced in version 0.19.0
-- -------------------------------------------------------------------------------------------------
\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Update vp_submission table
-- -------------------------------------------------------------------------------------------------
-- Add primary key constraint on request_id column
ALTER TABLE verify.vp_submission
ADD CONSTRAINT pk_vp_submission_request_id
PRIMARY KEY (request_id);

-- -------------------------------------------------------------------------------------------------
-- SECTION 2: Add dcql_query_scope table (v0.19.0+ uses this instead of presentation_definition)
-- -------------------------------------------------------------------------------------------------
-- Non-destructive migration: create the new table and retain verify.presentation_definition
-- until dcql_query_scope is populated and verified. v0.19.0 maps scopes via DcqlQueryScope.java;
-- presentation_definition rows cannot be converted to DCQL format in SQL alone.
CREATE TABLE IF NOT EXISTS verify.dcql_query_scope(
    scope character varying(255) NOT NULL,
    dcql_query text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dcql_query_scope_pkey PRIMARY KEY (scope)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'verify'
          AND table_name = 'presentation_definition'
    ) THEN
        RAISE NOTICE 'verify.presentation_definition retained for manual DCQL migration. Drop it only after verify.dcql_query_scope is populated and validated.';
    END IF;
END $$;