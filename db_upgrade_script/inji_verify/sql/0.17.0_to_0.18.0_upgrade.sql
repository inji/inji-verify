-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script : v0.17.0 to v0.18.0
-- Database       : inji_verify
-- Purpose        : Apply schema changes introduced in version 0.18.0
-- -------------------------------------------------------------------------------------------------
\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Update vp_submission table
-- -------------------------------------------------------------------------------------------------
-- Add the new columns for same-device flow support
ALTER TABLE vp_submission
    ADD COLUMN IF NOT EXISTS response_code CHARACTER VARYING(200),
    ADD COLUMN IF NOT EXISTS response_code_expiry_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS response_code_used BOOLEAN DEFAULT false,
    ADD CONSTRAINT uq_vp_submission_response_code UNIQUE (response_code);

-- Create the Index
CREATE INDEX IF NOT EXISTS idx_vp_submission_response_code ON vp_submission (response_code);