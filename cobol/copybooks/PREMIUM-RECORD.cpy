      ******************************************************************
      * PREMIUM-RECORD.CPY - Premium Calculation Record Layout
      * System:   Policy Administration System (PAS)
      * Author:   M. Kowalski
      * Date:     1998-05-20
      * Modified: 2008-01-15 - Added installment billing fields
      *           2019-11-01 - Added surcharge fields for regulatory
      * DB2 Table: PREMIUMS
      ******************************************************************
       01  PREMIUM-RECORD.
           05  PREM-POLICY-NUMBER       PIC X(12).
           05  PREM-COVERAGE-SEQ        PIC 9(03).
           05  PREM-TERM-EFF-DATE       PIC 9(08).
           05  PREM-TERM-EXP-DATE       PIC 9(08).
           05  PREM-BASE-RATE           PIC S9(07)V9999 COMP-3.
           05  PREM-TERRITORY-FACTOR    PIC S9(03)V9999 COMP-3.
           05  PREM-CLASS-FACTOR        PIC S9(03)V9999 COMP-3.
           05  PREM-EXPERIENCE-MOD      PIC S9(03)V9999 COMP-3.
           05  PREM-SCHEDULE-MOD        PIC S9(03)V9999 COMP-3.
           05  PREM-DISCOUNT-PCT        PIC S9(03)V99 COMP-3.
           05  PREM-SURCHARGE-AMT       PIC S9(07)V99 COMP-3.
           05  PREM-TAX-AMT             PIC S9(07)V99 COMP-3.
           05  PREM-TOTAL-PREMIUM       PIC S9(09)V99 COMP-3.
           05  PREM-INSTALLMENT-CODE    PIC X(02).
               88  PREM-ANNUAL          VALUE 'AN'.
               88  PREM-SEMI-ANNUAL     VALUE 'SA'.
               88  PREM-QUARTERLY       VALUE 'QT'.
               88  PREM-MONTHLY         VALUE 'MO'.
           05  PREM-INSTALLMENT-AMT     PIC S9(07)V99 COMP-3.
           05  PREM-CALC-DATE           PIC 9(08).
           05  PREM-CALC-BY             PIC X(08).
           05  FILLER                   PIC X(10).
