-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script : v0.14.0 to v0.15.0
-- Database       : :dbname
-- Purpose        : Apply schema changes introduced in version 0.15.0
-- -------------------------------------------------------------------------------------------------

\c :dbname

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Update vp_submission table
-- -------------------------------------------------------------------------------------------------
ALTER TABLE verify.vp_submission 
  ALTER COLUMN vp_token  DROP NOT NULL;

ALTER TABLE verify.vp_submission 
  ALTER COLUMN presentation_submission  DROP NOT NULL;

ALTER TABLE verify.vp_submission 
  ADD error character varying(100) NULL;

ALTER TABLE verify.vp_submission 
  ADD error_description character varying(200) NULL;

COMMENT ON COLUMN vp_submission.vp_token IS 'VP Token: base64 encoded VP submission result. This can be null, in case of error.';
COMMENT ON COLUMN vp_submission.presentation_submission IS 'Presentation Submission: presentation submission object which has details on where to find VC / Claims. This can be null, in case of error.';
COMMENT ON COLUMN vp_submission.error IS 'Error: error code as sent by wallet related to VP submission. This can be null, in case there is no error.';
COMMENT ON COLUMN vp_submission.error_description IS 'Error Description: error message as sent by wallet related to VP submission. This can be null, in case there is no error.';
-- -------------------------------------------------------------------------------------------------
-- END OF SCRIPT
-- -------------------------------------------------------------------------------------------------
