-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Rollback Script: v0.18.0 to v1.0.0
-- Release name   : 1.0.0-alpha.1
-- Database       : inji_verify
-- Purpose        : Revert schema changes introduced in version 1.0.0
-- -------------------------------------------------------------------------------------------------

\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Update vp_submission table
-- -------------------------------------------------------------------------------------------------
-- Drop primary key constraint on request_id column
ALTER TABLE vp_submission
DROP CONSTRAINT IF EXISTS pk_vp_submission_request_id;

-- -------------------------------------------------------------------------------------------------
-- SECTION 2: Recreate presentation_definition table
-- -------------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS presentation_definition(
    id character varying(36) NOT NULL,
    input_descriptors text NOT NULL,
    name character varying(500),
    purpose character varying(500),
    vp_format text,
    submission_requirements text
);