//PREMBAT  JOB (ACME,PAS),'PREMIUM BATCH CALC',
//         CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1),
//         NOTIFY=&SYSUID,TIME=(1,)
//*******************************************************************
//* PREMIUM-BATCH.JCL - Premium Batch Calculation
//* System:   Policy Administration System (PAS)
//* Schedule: Daily at 01:00 EST (CA-7 Job# PAS0050)
//* Author:   M. Kowalski
//* Date:     1998-08-01
//* Modified: 2019-11-15 - Added surcharge step
//*
//* Runs the PREMBAT COBOL batch program to recalculate
//* premiums for all active policies.
//*******************************************************************
//*
//JOBLIB   DD DSN=ACME.PAS.LOADLIB,DISP=SHR
//         DD DSN=DB2.V12.SDSNLOAD,DISP=SHR
//*
//*-------------------------------------------------------------------
//* STEP010 - Run premium batch calculation
//*-------------------------------------------------------------------
//STEP010  EXEC PGM=IKJEFT01,DYNAMNBR=20
//STEPLIB  DD DSN=DB2.V12.SDSNLOAD,DISP=SHR
//         DD DSN=ACME.PAS.LOADLIB,DISP=SHR
//SYSTSPRT DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//PREMRPT  DD DSN=ACME.PAS.REPORTS.PREMIUM.D&LYYMMDD,
//         DISP=(NEW,CATLG,DELETE),
//         UNIT=SYSDA,SPACE=(CYL,(10,5),RLSE),
//         DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSTSIN  DD *
  DSN SYSTEM(DBPD)
  RUN PROGRAM(PREMBAT) PLAN(PASBATCH) -
      LIB('ACME.PAS.LOADLIB')
  END
/*
//*
//*-------------------------------------------------------------------
//* STEP020 - Check return code and send email
//*-------------------------------------------------------------------
//STEP020  EXEC PGM=IEFBR14,COND=(0,NE,STEP010)
//* If step010 RC=0, batch completed successfully
//*
//STEP030  EXEC PGM=IKJEFT01,COND=(4,LT,STEP010)
//SYSTSPRT DD SYSOUT=*
//SYSTSIN  DD *
  SEND 'PREMIUM BATCH COMPLETED WITH WARNINGS' USER(PASADMIN)
/*
//*
