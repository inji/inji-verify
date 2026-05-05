-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Upgrade Script : v0.18.0 to v0.19.0
-- Database       : inji_verify
-- Purpose        : Remove the presentation_definition table (no longer maintained server-side).
-- -------------------------------------------------------------------------------------------------
\c inji_verify

-- -------------------------------------------------------------------------------------------------
-- SECTION 1: Drop presentation_definition table
-- -------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS verify.presentation_definition;
