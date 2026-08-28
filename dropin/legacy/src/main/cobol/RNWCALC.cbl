      ******************************************************************
      * RNWCALC - renewal arithmetic from POLRNW, compilable off z/OS
      *
      * The paragraphs below are copied from cobol/programs/POLRNW.cbl
      * (4000-CALCULATE-NEW-PREMIUM, 5000-APPLY-RATE-CAP and the date
      * arithmetic of 6000-CREATE-RENEWAL-TERM) with the EXEC CICS and
      * EXEC SQL statements removed, because those need a CICS region
      * and a DB2 subsystem and this needs to run on a laptop.
      *
      * Nothing else is changed: the picture clauses, the literals and
      * the COMPUTE statements are the incumbent's. That is the point -
      * it lets dropin/legacy's Java transliteration be checked against
      * a real COBOL compiler (GnuCOBOL) instead of against a reading
      * of the source.
      *
      * Reads three lines on stdin: premium, expiry date (YYYYMMDD),
      * renewal count. Writes one line: premium|expiry|count|pct.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. RNWCALC.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-IN-PREMIUM             PIC X(15).
       01  WS-IN-EXPIRY              PIC X(08).
       01  WS-IN-COUNT               PIC X(03).

       01  WS-RATE-INCREASE-CAP      PIC S9(03)V99 COMP-3
                                     VALUE 15.00.
       01  WS-RATE-CHANGE-PCT        PIC S9(03)V99 COMP-3.
       01  WS-NEW-PREMIUM            PIC S9(09)V99 COMP-3.
       01  WS-OLD-PREMIUM            PIC S9(09)V99 COMP-3.
       01  WS-NEW-EXPIRY-DATE        PIC 9(08).

       01  POLICY-TOTAL-PREMIUM      PIC S9(09)V99 COMP-3.
       01  POLICY-EFFECTIVE-DATE     PIC 9(08).
       01  POLICY-EXPIRY-DATE        PIC 9(08).
       01  POLICY-RENEWAL-COUNT      PIC 9(03).

       01  WS-OUT-PREMIUM            PIC 9(09).99.
       01  WS-OUT-PCT                PIC 9(03).99.

       PROCEDURE DIVISION.
       0000-MAIN.
           PERFORM 0100-READ-INPUT
           PERFORM 4000-CALCULATE-NEW-PREMIUM
           PERFORM 5000-APPLY-RATE-CAP
           PERFORM 6000-CREATE-RENEWAL-TERM
           PERFORM 0900-WRITE-OUTPUT
           STOP RUN
           .

       0100-READ-INPUT.
           ACCEPT WS-IN-PREMIUM
           ACCEPT WS-IN-EXPIRY
           ACCEPT WS-IN-COUNT
           COMPUTE POLICY-TOTAL-PREMIUM =
               FUNCTION NUMVAL(WS-IN-PREMIUM)
           COMPUTE POLICY-EXPIRY-DATE =
               FUNCTION NUMVAL(WS-IN-EXPIRY)
           COMPUTE POLICY-RENEWAL-COUNT =
               FUNCTION NUMVAL(WS-IN-COUNT)
           .

       4000-CALCULATE-NEW-PREMIUM.
           MOVE POLICY-TOTAL-PREMIUM TO WS-OLD-PREMIUM
           COMPUTE WS-NEW-PREMIUM =
               WS-OLD-PREMIUM * 1.05
           COMPUTE WS-RATE-CHANGE-PCT =
               ((WS-NEW-PREMIUM - WS-OLD-PREMIUM)
                / WS-OLD-PREMIUM) * 100
           .

       5000-APPLY-RATE-CAP.
           IF WS-RATE-CHANGE-PCT > WS-RATE-INCREASE-CAP
               COMPUTE WS-NEW-PREMIUM =
                   WS-OLD-PREMIUM * (1 + WS-RATE-INCREASE-CAP / 100)
           END-IF
           MOVE WS-NEW-PREMIUM TO POLICY-TOTAL-PREMIUM
           .

       6000-CREATE-RENEWAL-TERM.
           ADD 1 TO POLICY-RENEWAL-COUNT
           MOVE POLICY-EXPIRY-DATE TO POLICY-EFFECTIVE-DATE
           ADD 10000 TO POLICY-EXPIRY-DATE
                        GIVING WS-NEW-EXPIRY-DATE
           MOVE WS-NEW-EXPIRY-DATE TO POLICY-EXPIRY-DATE
           .

       0900-WRITE-OUTPUT.
           MOVE POLICY-TOTAL-PREMIUM TO WS-OUT-PREMIUM
           MOVE WS-RATE-CHANGE-PCT TO WS-OUT-PCT
           DISPLAY WS-OUT-PREMIUM '|' POLICY-EXPIRY-DATE '|'
                   POLICY-RENEWAL-COUNT '|' WS-OUT-PCT
           .
