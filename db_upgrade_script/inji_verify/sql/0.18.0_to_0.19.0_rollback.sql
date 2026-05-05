-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.

-- -------------------------------------------------------------------------------------------------
-- Rollback Script: v0.18.0 to v0.19.0
-- Database       : inji_verify
-- Purpose        : Restore the presentation_definition table (empty).
-- -------------------------------------------------------------------------------------------------
\c inji_verify

CREATE TABLE IF NOT EXISTS verify.presentation_definition(
    id character varying(36) NOT NULL,
    input_descriptors text NOT NULL,
    name character varying(500),
    purpose character varying(500),
    vp_format text,
    submission_requirements text
);

COMMENT ON TABLE verify.presentation_definition IS 'Presentation Definition table: Store details of predefined Presentation Definitions used in openID4VP sharing';
COMMENT ON COLUMN verify.presentation_definition.id IS 'ID: The field should provide a unique ID for the desired context';
COMMENT ON COLUMN verify.presentation_definition.input_descriptors IS 'Input Descriptors: Input Descriptors Objects are populated with properties describing what type of input data/Claim';
COMMENT ON COLUMN verify.presentation_definition.name IS 'Name: this should be a human-friendly string intended to constitute a distinctive designation of the Presentation Definition';
COMMENT ON COLUMN verify.presentation_definition.purpose IS 'Purpose: this describes the purpose for which the Presentation Definition inputs are being used for.';
COMMENT ON COLUMN verify.presentation_definition.vp_format IS 'Format: this describes which algorithms the Verifier supports for the format.';
COMMENT ON COLUMN verify.presentation_definition.submission_requirements IS 'Submission Requirements: express what combinations of inputs must be submitted to comply with its requirements for proceeding in a flow';
