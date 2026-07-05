-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Rollback Script: v0.13.1 to v0.13.0
-- Database       : :dbname
-- Purpose        : Revert schema changes introduced in version 0.13.1
-- -------------------------------------------------------------------------------------------------

\c :dbname

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Drop new tables
-- -------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS verify.vc_submission;

-- -------------------------------------------------------------------------------------------------
-- END OF SCRIPT
-- -------------------------------------------------------------------------------------------------
