-- This Source Code Form is subject to the terms of the Mozilla Public
-- License, v. 2.0. If a copy of the MPL was not distributed with this
-- file, You can obtain one at https://mozilla.org/MPL/2.0/.
-- -------------------------------------------------------------------------------------------------
-- Database Name: inji_verify
-- Table Name : vp_submission
-- Purpose    : VP Submission
--
--
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- ------------------------------------------------------------------------------------------
CREATE TABLE vp_submission(
                            request_id character varying(40) NOT NULL,
                            vp_token VARCHAR NULL,
                            presentation_submission text NULL,
                            error character varying(100) NULL,
                            error_description character varying(200) NULL,
                            response_code character varying(200) NULL,
                            response_code_expiry_at TIMESTAMP WITH TIME ZONE NULL,
                            response_code_used boolean DEFAULT false,
                            CONSTRAINT uq_vp_submission_response_code UNIQUE (response_code)
);

CREATE INDEX IF NOT EXISTS idx_vp_submission_response_code ON vp_submission (response_code);
COMMENT ON TABLE vp_submission IS 'VP Submission table: Store details of all the verifiable presentation submissions';
COMMENT ON COLUMN vp_submission.request_id IS 'Request ID: request ID verifiable presentation submission';
COMMENT ON COLUMN vp_submission.vp_token IS 'VP Token: base64 encoded VP submission result. This can be null, in case of error.';
COMMENT ON COLUMN vp_submission.presentation_submission IS 'Presentation Submission: presentation submission object which has details on where to find VC / Claims. This can be null, in case of error.';
COMMENT ON COLUMN vp_submission.error IS 'Error: error code as sent by wallet related to VP submission. This can be null, in case there is no error.';
COMMENT ON COLUMN vp_submission.error_description IS 'Error Description: error message as sent by wallet related to VP submission. This can be null, in case there is no error.';
COMMENT ON COLUMN vp_submission.response_code IS 'Response Code: A short-lived, one-time credential used to ensure only the authorized receiver can fetch the Verifiable Presentation response.';
COMMENT ON COLUMN vp_submission.response_code_expiry_at IS 'Response Code Expiry At: The UTC timestamp defining the end of the validity window for the response code.';
COMMENT ON COLUMN vp_submission.response_code_used IS 'Response Code Used: A boolean flag for replay protection. Ensures that the response code is consumed exactly once and cannot be reused.';