-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Rollback Script: v0.15.0 to v0.14.0
-- Database       : :dbname
-- Purpose        : Revert schema changes introduced in version 0.15.0
-- -------------------------------------------------------------------------------------------------

\c :dbname

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Revert vp_submission table
-- -------------------------------------------------------------------------------------------------

ALTER TABLE verify.vp_submission 
  ALTER COLUMN vp_token  SET NOT NULL;

ALTER TABLE verify.vp_submission 
  ALTER COLUMN presentation_submission  SET NOT NULL;

ALTER TABLE verify.vp_submission 
  DROP COLUMN error;

ALTER TABLE verify.vp_submission 
  DROP COLUMN error_description;

COMMENT ON COLUMN vp_submission.vp_token IS 'VP Token: base64 encoded VP submission result.';
COMMENT ON COLUMN vp_submission.presentation_submission IS 'Presentation Submission: presentation submission object which has details on where to find VC / Claims.';

-- -------------------------------------------------------------------------------------------------
-- END OF SCRIPT
-- -------------------------------------------------------------------------------------------------
