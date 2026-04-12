      ******************************************************************
      * CUSTOMER-RECORD.CPY - Customer/Policyholder Record Layout
      * System:   Policy Administration System (PAS)
      * Author:   J. Henderson
      * Date:     1998-03-15
      * Modified: 2006-08-01 - Added email and phone fields
      *           2018-05-25 - Added GDPR consent flag
      * DB2 Table: POLICY_HOLDERS
      ******************************************************************
       01  CUSTOMER-RECORD.
           05  CUST-ID                  PIC X(10).
           05  CUST-TYPE                PIC X(01).
               88  CUST-INDIVIDUAL      VALUE 'I'.
               88  CUST-COMMERCIAL      VALUE 'C'.
               88  CUST-GOVERNMENT      VALUE 'G'.
           05  CUST-LAST-NAME           PIC X(30).
           05  CUST-FIRST-NAME          PIC X(20).
           05  CUST-MIDDLE-INIT         PIC X(01).
           05  CUST-COMPANY-NAME        PIC X(50).
           05  CUST-ADDRESS.
               10  CUST-ADDR-LINE1      PIC X(40).
               10  CUST-ADDR-LINE2      PIC X(40).
               10  CUST-CITY            PIC X(25).
               10  CUST-STATE           PIC X(02).
               10  CUST-ZIP             PIC X(10).
               10  CUST-COUNTRY         PIC X(03).
           05  CUST-PHONE               PIC X(15).
           05  CUST-EMAIL               PIC X(60).
           05  CUST-DOB                 PIC 9(08).
           05  CUST-SSN-LAST4           PIC X(04).
           05  CUST-TAX-ID              PIC X(10).
           05  CUST-CREDIT-SCORE        PIC 9(03).
           05  CUST-RISK-TIER           PIC X(01).
               88  CUST-TIER-PREFERRED  VALUE 'P'.
               88  CUST-TIER-STANDARD   VALUE 'S'.
               88  CUST-TIER-SUBSTAND   VALUE 'U'.
           05  CUST-GDPR-CONSENT        PIC X(01).
               88  CUST-GDPR-YES        VALUE 'Y'.
               88  CUST-GDPR-NO         VALUE 'N'.
           05  CUST-CREATED-DATE        PIC 9(08).
           05  CUST-LAST-UPDATED        PIC 9(08).
           05  FILLER                   PIC X(15).
