-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Rollback Script: v0.18.0 to v0.19.0
-- Database       : inji_verify
-- Purpose        : Revert schema changes introduced in version 0.19.0
-- -------------------------------------------------------------------------------------------------

\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Update vp_submission table
-- -------------------------------------------------------------------------------------------------
-- Drop primary key constraint on request_id column
ALTER TABLE verify.vp_submission
DROP CONSTRAINT IF EXISTS pk_vp_submission_request_id;

-- -------------------------------------------------------------------------------------------------
-- SECTION 2: Revert dcql_query_scope to presentation_definition
-- -------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS verify.dcql_query_scope;

-- Recreate presentation_definition only when the upgrade retained the legacy table.
CREATE TABLE IF NOT EXISTS verify.presentation_definition (
    id character varying(255) NOT NULL,
    input_descriptors text,
    name character varying(255),
    purpose character varying(255),
    vp_format text,
    submission_requirements text,
    CONSTRAINT presentation_definition_pkey PRIMARY KEY (id)
);  