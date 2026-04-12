       IDENTIFICATION DIVISION.
       PROGRAM-ID. PREMBAT.
      ******************************************************************
      * PREMBAT - Premium Calculation Batch Program
      * System:   Policy Administration System (PAS)
      * Author:   M. Kowalski
      * Date:     1998-07-01
      * Modified: 2003-01-15 - Added territory rating factors
      *           2010-04-01 - Added experience modification
      *           2019-11-15 - Added regulatory surcharges
      *
      * Batch program to calculate premiums for all active policies.
      * Reads policy and coverage data, applies rating factors,
      * experience mods, and surcharges. Writes to PREMIUMS table.
      * Called by JCL job PREMIUM-BATCH.jcl.
      *
      * NOTE: This is a batch program - NO CICS commands.
      * Runs in z/OS batch region.
      ******************************************************************
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT PREMIUM-REPORT
               ASSIGN TO PREMRPT
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS IS WS-FILE-STATUS.
       DATA DIVISION.
       FILE SECTION.
       FD  PREMIUM-REPORT
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS
           RECORD CONTAINS 132 CHARACTERS.
       01  PREMIUM-REPORT-REC         PIC X(132).

       WORKING-STORAGE SECTION.
       01  WS-PROGRAM-ID             PIC X(08) VALUE 'PREMBAT'.
       01  WS-FILE-STATUS            PIC X(02).
       01  WS-SQLCODE                PIC S9(09) COMP-5.
       01  WS-POLICIES-READ          PIC 9(07) VALUE 0.
       01  WS-POLICIES-UPDATED       PIC 9(07) VALUE 0.
       01  WS-POLICIES-ERROR         PIC 9(07) VALUE 0.
       01  WS-CURRENT-DATE           PIC 9(08).
       01  WS-REPORT-LINE            PIC X(132).
       01  WS-RETURN-CODE            PIC S9(04) COMP VALUE 0.

       COPY POLICY-RECORD.
       COPY COVERAGE-RECORD.
       COPY PREMIUM-RECORD.

      * Rating factor table - loaded from DB2
       01  WS-TERRITORY-TABLE.
           05  WS-TERR-ENTRY OCCURS 100 TIMES.
               10  WS-TERR-CODE      PIC X(06).
               10  WS-TERR-FACTOR    PIC S9(03)V9999 COMP-3.

       01  WS-CLASS-TABLE.
           05  WS-CLASS-ENTRY OCCURS 200 TIMES.
               10  WS-CLS-CODE       PIC X(05).
               10  WS-CLS-FACTOR     PIC S9(03)V9999 COMP-3.

       01  WS-CALC-FIELDS.
           05  WS-BASE-PREMIUM       PIC S9(09)V99 COMP-3.
           05  WS-TERR-PREMIUM       PIC S9(09)V99 COMP-3.
           05  WS-CLASS-PREMIUM      PIC S9(09)V99 COMP-3.
           05  WS-MOD-PREMIUM        PIC S9(09)V99 COMP-3.
           05  WS-TAX-AMOUNT         PIC S9(07)V99 COMP-3.
           05  WS-SURCHARGE          PIC S9(07)V99 COMP-3.
           05  WS-FINAL-PREMIUM      PIC S9(09)V99 COMP-3.

      * Hardcoded tax rate - TODO: make state-specific
       01  WS-TAX-RATE               PIC S9(01)V9999 COMP-3
           VALUE 0.0350.

           EXEC SQL INCLUDE SQLCA END-EXEC.

       PROCEDURE DIVISION.
       0000-MAIN-LOGIC.
           PERFORM 1000-INITIALIZE
           PERFORM 2000-LOAD-RATING-TABLES
           PERFORM 3000-PROCESS-POLICIES
           PERFORM 4000-WRITE-SUMMARY
           PERFORM 9999-TERMINATE
           STOP RUN.

       1000-INITIALIZE.
           ACCEPT WS-CURRENT-DATE FROM DATE YYYYMMDD
           OPEN OUTPUT PREMIUM-REPORT
           IF WS-FILE-STATUS NOT = '00'
               DISPLAY 'ERROR OPENING REPORT FILE: ' WS-FILE-STATUS
               MOVE 16 TO WS-RETURN-CODE
               STOP RUN
           END-IF
           MOVE SPACES TO WS-REPORT-LINE
           STRING 'ACME INSURANCE - PREMIUM BATCH CALCULATION'
                  ' RUN DATE: ' WS-CURRENT-DATE
               DELIMITED BY SIZE INTO WS-REPORT-LINE
           WRITE PREMIUM-REPORT-REC FROM WS-REPORT-LINE
           MOVE ALL '-' TO WS-REPORT-LINE
           WRITE PREMIUM-REPORT-REC FROM WS-REPORT-LINE
           .

       2000-LOAD-RATING-TABLES.
      *    Load territory factors from DB2
           EXEC SQL
               DECLARE TERR_CURSOR CURSOR FOR
               SELECT TERRITORY_CODE, RATING_FACTOR
               FROM TERRITORY_FACTORS
               WHERE EFFECTIVE_DATE <= :WS-CURRENT-DATE
               ORDER BY TERRITORY_CODE
           END-EXEC
           EXEC SQL OPEN TERR_CURSOR END-EXEC
      *    Load into working storage table
      *    (simplified - production loads all rows)
           EXEC SQL CLOSE TERR_CURSOR END-EXEC
           .

       3000-PROCESS-POLICIES.
           EXEC SQL
               DECLARE POL_CURSOR CURSOR FOR
               SELECT POLICY_NUMBER, POLICY_TYPE,
                      TOTAL_PREMIUM, DEDUCTIBLE,
                      COVERAGE_LIMIT,
                      EFFECTIVE_DATE, EXPIRY_DATE
               FROM POLICIES
               WHERE POLICY_STATUS = 'AC'
               ORDER BY POLICY_NUMBER
           END-EXEC
           EXEC SQL OPEN POL_CURSOR END-EXEC
           PERFORM 3100-FETCH-POLICY
               UNTIL SQLCODE NOT = 0
           EXEC SQL CLOSE POL_CURSOR END-EXEC
           .

       3100-FETCH-POLICY.
           EXEC SQL
               FETCH POL_CURSOR
               INTO :POLICY-NUMBER, :POLICY-TYPE,
                    :POLICY-TOTAL-PREMIUM, :POLICY-DEDUCTIBLE,
                    :POLICY-LIMIT,
                    :POLICY-EFFECTIVE-DATE, :POLICY-EXPIRY-DATE
           END-EXEC
           IF SQLCODE = 0
               ADD 1 TO WS-POLICIES-READ
               PERFORM 3200-CALCULATE-PREMIUM
               PERFORM 3300-WRITE-PREMIUM-RECORD
           END-IF
           .

       3200-CALCULATE-PREMIUM.
      *    Base rate lookup by policy type
           EVALUATE TRUE
               WHEN POL-TYPE-AUTO
                   MOVE 850.00 TO WS-BASE-PREMIUM
               WHEN POL-TYPE-HOME
                   MOVE 1200.00 TO WS-BASE-PREMIUM
               WHEN POL-TYPE-COMM
                   MOVE 5000.00 TO WS-BASE-PREMIUM
               WHEN POL-TYPE-LIFE
                   MOVE 400.00 TO WS-BASE-PREMIUM
               WHEN POL-TYPE-HEALTH
                   MOVE 3500.00 TO WS-BASE-PREMIUM
               WHEN OTHER
                   MOVE 1000.00 TO WS-BASE-PREMIUM
           END-EVALUATE

      *    Apply territory factor (using 1.0 default)
           COMPUTE WS-TERR-PREMIUM =
               WS-BASE-PREMIUM * 1.00

      *    Apply class factor
           COMPUTE WS-CLASS-PREMIUM =
               WS-TERR-PREMIUM * 1.00

      *    Apply experience modification
           COMPUTE WS-MOD-PREMIUM =
               WS-CLASS-PREMIUM * 1.00

      *    Calculate tax
           COMPUTE WS-TAX-AMOUNT =
               WS-MOD-PREMIUM * WS-TAX-RATE

      *    Surcharge (regulatory - flat $25 per policy)
           MOVE 25.00 TO WS-SURCHARGE

      *    Final premium
           COMPUTE WS-FINAL-PREMIUM =
               WS-MOD-PREMIUM + WS-TAX-AMOUNT + WS-SURCHARGE
           .

       3300-WRITE-PREMIUM-RECORD.
           MOVE POLICY-NUMBER TO PREM-POLICY-NUMBER
           MOVE 1             TO PREM-COVERAGE-SEQ
           MOVE POLICY-EFFECTIVE-DATE TO PREM-TERM-EFF-DATE
           MOVE POLICY-EXPIRY-DATE    TO PREM-TERM-EXP-DATE
           MOVE WS-BASE-PREMIUM       TO PREM-BASE-RATE
           MOVE WS-TAX-AMOUNT         TO PREM-TAX-AMT
           MOVE WS-SURCHARGE          TO PREM-SURCHARGE-AMT
           MOVE WS-FINAL-PREMIUM      TO PREM-TOTAL-PREMIUM
           MOVE WS-CURRENT-DATE       TO PREM-CALC-DATE
           MOVE 'PREMBAT'             TO PREM-CALC-BY

           EXEC SQL
               INSERT INTO PREMIUMS
               (POLICY_NUMBER, COVERAGE_SEQ,
                TERM_EFFECTIVE_DATE, TERM_EXPIRY_DATE,
                BASE_RATE, TERRITORY_FACTOR, CLASS_FACTOR,
                EXPERIENCE_MOD, SCHEDULE_MOD,
                DISCOUNT_PCT, SURCHARGE_AMT, TAX_AMT,
                TOTAL_PREMIUM, INSTALLMENT_CODE,
                CALC_DATE, CALC_BY)
               VALUES
               (:PREM-POLICY-NUMBER, :PREM-COVERAGE-SEQ,
                :PREM-TERM-EFF-DATE, :PREM-TERM-EXP-DATE,
                :PREM-BASE-RATE, 1.0000, 1.0000,
                1.0000, 1.0000,
                0.00, :PREM-SURCHARGE-AMT, :PREM-TAX-AMT,
                :PREM-TOTAL-PREMIUM, 'AN',
                :PREM-CALC-DATE, :PREM-CALC-BY)
           END-EXEC
           IF SQLCODE = 0
               ADD 1 TO WS-POLICIES-UPDATED
           ELSE
               ADD 1 TO WS-POLICIES-ERROR
           END-IF

      *    Write report line
           MOVE SPACES TO WS-REPORT-LINE
           STRING POLICY-NUMBER ' PREMIUM: '
                  WS-FINAL-PREMIUM
               DELIMITED BY SIZE INTO WS-REPORT-LINE
           WRITE PREMIUM-REPORT-REC FROM WS-REPORT-LINE
           .

       4000-WRITE-SUMMARY.
           MOVE SPACES TO WS-REPORT-LINE
           MOVE ALL '=' TO WS-REPORT-LINE
           WRITE PREMIUM-REPORT-REC FROM WS-REPORT-LINE
           MOVE SPACES TO WS-REPORT-LINE
           STRING 'TOTAL POLICIES READ:    ' WS-POLICIES-READ
               DELIMITED BY SIZE INTO WS-REPORT-LINE
           WRITE PREMIUM-REPORT-REC FROM WS-REPORT-LINE
           MOVE SPACES TO WS-REPORT-LINE
           STRING 'TOTAL POLICIES UPDATED: ' WS-POLICIES-UPDATED
               DELIMITED BY SIZE INTO WS-REPORT-LINE
           WRITE PREMIUM-REPORT-REC FROM WS-REPORT-LINE
           MOVE SPACES TO WS-REPORT-LINE
           STRING 'TOTAL ERRORS:           ' WS-POLICIES-ERROR
               DELIMITED BY SIZE INTO WS-REPORT-LINE
           WRITE PREMIUM-REPORT-REC FROM WS-REPORT-LINE
           .

       9999-TERMINATE.
           CLOSE PREMIUM-REPORT
           IF WS-POLICIES-ERROR > 0
               MOVE 4 TO WS-RETURN-CODE
           END-IF
           MOVE WS-RETURN-CODE TO RETURN-CODE
           .
