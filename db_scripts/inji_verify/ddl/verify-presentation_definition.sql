-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.
-- -------------------------------------------------------------------------------------------------
-- Database Name: inji_verify
-- Table Name : presentation_definition
-- Purpose    : Presentation Definition table
--
--
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- ------------------------------------------------------------------------------------------
CREATE TABLE presentation_definition(
                                                      id character varying(36) NOT NULL,
                                                      input_descriptors text NOT NULL,
                                                      name character varying(500),
                                                      purpose character varying(500),
                                                      vp_format text,
                                                      submission_requirements text
);

COMMENT ON TABLE presentation_definition IS 'Presentation Definition table: Store details of predefined Presentation Definitions used in openID4VP sharing';
COMMENT ON COLUMN presentation_definition.id IS 'ID: The field should provide a unique ID for the desired context';
COMMENT ON COLUMN presentation_definition.input_descriptors IS 'Input Descriptors: Input Descriptors Objects are populated with properties describing what type of input data/Claim';
COMMENT ON COLUMN presentation_definition.name IS 'Name: this should be a human-friendly string intended to constitute a distinctive designation of the Presentation Definition';
COMMENT ON COLUMN presentation_definition.purpose IS 'Purpose: this describes the purpose for which the Presentation Definition inputs are being used for.';
COMMENT ON COLUMN presentation_definition.vp_format IS 'Format: this describes which algorithms the Verifier supports for the format.';
COMMENT ON COLUMN presentation_definition.submission_requirements IS 'Submission Requirements: express what combinations of inputs must be submitted to comply with its requirements for proceeding in a flow';