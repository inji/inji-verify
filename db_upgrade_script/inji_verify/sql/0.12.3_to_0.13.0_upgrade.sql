-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script: v0.12.3 to v0.13.0
-- Database       : :dbname
-- Purpose        : Apply schema changes introduced in version 0.13.0
-- -------------------------------------------------------------------------------------------------

\c :dbname

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Alter column types (from JSON/JSONB to TEXT)
-- -------------------------------------------------------------------------------------------------

ALTER TABLE verify.authorization_request_details 
  ALTER COLUMN authorization_details 
  TYPE TEXT 
  USING authorization_details::TEXT;

ALTER TABLE verify.presentation_definition 
  ALTER COLUMN vp_format 
  TYPE TEXT 
  USING vp_format::TEXT;

ALTER TABLE verify.presentation_definition 
  ALTER COLUMN submission_requirements 
  TYPE TEXT 
  USING submission_requirements::TEXT;

ALTER TABLE verify.vp_submission 
  ALTER COLUMN presentation_submission 
  TYPE TEXT 
  USING presentation_submission::TEXT;

-- -------------------------------------------------------------------------------------------------
-- END OF SCRIPT
-- -------------------------------------------------------------------------------------------------