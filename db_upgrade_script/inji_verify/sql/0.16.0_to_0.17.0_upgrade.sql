-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script : v0.16.0 to v0.17.0
-- Database       : inji_verify
-- Purpose        : Apply schema changes introduced in version 0.16.0
-- -------------------------------------------------------------------------------------------------
\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Update presentation_definition table
-- -------------------------------------------------------------------------------------------------
ALTER TABLE verify.presentation_definition
    ALTER COLUMN input_descriptors TYPE TEXT;