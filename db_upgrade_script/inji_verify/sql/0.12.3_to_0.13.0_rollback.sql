-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Rollback Script: v0.13.0 to v0.12.3
-- Database       : :dbname
-- Purpose        : Revert schema changes introduced in version 0.13.0
-- -------------------------------------------------------------------------------------------------

\c :dbname

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Revert column types (from TEXT back to JSONB)
-- -------------------------------------------------------------------------------------------------

ALTER TABLE verify.authorization_request_details 
  ALTER COLUMN authorization_details 
  TYPE JSONB 
  USING authorization_details::JSONB;

ALTER TABLE verify.presentation_definition 
  ALTER COLUMN vp_format 
  TYPE JSONB 
  USING vp_format::JSONB;

ALTER TABLE verify.presentation_definition 
  ALTER COLUMN submission_requirements 
  TYPE JSONB 
  USING submission_requirements::JSONB;

ALTER TABLE verify.vp_submission 
  ALTER COLUMN presentation_submission 
  TYPE JSONB 
  USING presentation_submission::JSONB;

-- -------------------------------------------------------------------------------------------------
-- END OF SCRIPT
-- -------------------------------------------------------------------------------------------------
