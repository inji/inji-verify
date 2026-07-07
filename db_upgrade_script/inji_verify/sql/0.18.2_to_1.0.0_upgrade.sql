-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script : v0.18.0 to v1.0.0
-- Release name   : 1.0.0-alpha.1
-- Database       : inji_verify
-- Purpose        : Apply schema changes introduced in version 1.0.0
-- -------------------------------------------------------------------------------------------------
\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Update vp_submission table
-- -------------------------------------------------------------------------------------------------
-- Add primary key constraint on request_id column (with duplicate and idempotency checks)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'pk_vp_submission_request_id'
    ) THEN
        RAISE NOTICE 'Constraint pk_vp_submission_request_id already exists, skipping.';
    ELSE
        IF EXISTS (
            SELECT request_id FROM vp_submission
            GROUP BY request_id HAVING COUNT(*) > 1
        ) THEN
            RAISE EXCEPTION 'Duplicate request_id values found in vp_submission. Resolve duplicates before applying this migration.';
        END IF;

        ALTER TABLE vp_submission
        ADD CONSTRAINT pk_vp_submission_request_id
        PRIMARY KEY (request_id);
    END IF;
END $$;

-- -------------------------------------------------------------------------------------------------
-- SECTION 2: Drop presentation_definition table
-- -------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS presentation_definition;