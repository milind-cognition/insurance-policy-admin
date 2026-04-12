      ******************************************************************
      * COVERAGE-RECORD.CPY - Coverage/Line of Business Record Layout
      * System:   Policy Administration System (PAS)
      * Author:   J. Henderson
      * Date:     1998-03-15
      * Modified: 2003-09-10 - Added terrorism coverage types
      *           2010-04-22 - Added cyber liability coverage
      * DB2 Table: COVERAGES
      ******************************************************************
       01  COVERAGE-RECORD.
           05  COV-POLICY-NUMBER        PIC X(12).
           05  COV-SEQUENCE-NUM         PIC 9(03).
           05  COV-TYPE-CODE            PIC X(04).
               88  COV-PROPERTY         VALUE 'PROP'.
               88  COV-LIABILITY         VALUE 'LIAB'.
               88  COV-AUTO-PHYS        VALUE 'AUTP'.
               88  COV-AUTO-LIAB        VALUE 'AUTL'.
               88  COV-WORKERS-COMP     VALUE 'WKCP'.
               88  COV-UMBRELLA         VALUE 'UMBR'.
               88  COV-CYBER            VALUE 'CYBR'.
               88  COV-TERRORISM        VALUE 'TERR'.
           05  COV-DESCRIPTION          PIC X(40).
           05  COV-LIMIT                PIC S9(11)V99 COMP-3.
           05  COV-DEDUCTIBLE           PIC S9(07)V99 COMP-3.
           05  COV-PREMIUM              PIC S9(09)V99 COMP-3.
           05  COV-EFFECTIVE-DATE       PIC 9(08).
           05  COV-EXPIRY-DATE          PIC 9(08).
           05  COV-STATUS               PIC X(02).
               88  COV-STAT-ACTIVE      VALUE 'AC'.
               88  COV-STAT-PENDING     VALUE 'PN'.
               88  COV-STAT-CANCELLED   VALUE 'CN'.
           05  COV-COINSURANCE-PCT      PIC 9(03).
           05  COV-RATING-TERRITORY     PIC X(06).
           05  COV-CLASS-CODE           PIC X(05).
           05  FILLER                   PIC X(15).
