-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Rollback Script: v0.17.0 to v0.18.0
-- Database       : inji_verify
-- Purpose        : Revert schema changes introduced in version 0.18.0
-- -------------------------------------------------------------------------------------------------

\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Revert vp_submission table
-- -------------------------------------------------------------------------------------------------
-- Remove the columns added for same-device flow support
-- 1. Remove the Index
DROP INDEX IF EXISTS verify.idx_vp_submission_response_code;

-- 2. Drop the columns (this automatically removes the associated comments)
ALTER TABLE verify.vp_submission
    DROP COLUMN IF EXISTS response_code,
    DROP COLUMN IF EXISTS response_code_expiry_at,
    DROP COLUMN IF EXISTS response_code_used,
    DROP CONSTRAINT IF EXISTS uq_vp_submission_response_code;