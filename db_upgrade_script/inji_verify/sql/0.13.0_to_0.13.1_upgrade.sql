-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script : v0.13.1 to v0.13.0
-- Database       : :dbname
-- Purpose        : Apply schema changes introduced in version 0.13.1
-- -------------------------------------------------------------------------------------------------

\c :dbname

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Create vc_submission table
-- -------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS verify.vc_submission (
  transaction_id VARCHAR(40) NOT NULL,
  vc TEXT NOT NULL
);

COMMENT ON TABLE verify.vc_submission IS 
  'VC Submission table: Stores details of all verifiable credentials submissions';

COMMENT ON COLUMN verify.vc_submission.transaction_id IS 
  'Transaction ID: Unique identifier for the VC submission transaction';

COMMENT ON COLUMN verify.vc_submission.vc IS 
  'VC: Base64-encoded verifiable credential submission result';

-- -------------------------------------------------------------------------------------------------
-- END OF SCRIPT
-- -------------------------------------------------------------------------------------------------
