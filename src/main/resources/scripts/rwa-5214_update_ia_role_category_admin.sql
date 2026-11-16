--Connect to main database

-- 1. Select only the tasks identified by IA team (should return 37)
select count(*) from cft_task_db.tasks where tasks.jurisdiction='IA'
                                         and role_category = 'CTSC'
                                         and state = 'UNASSIGNED'
                                         and task_type = 'detainedListTheCase'
                                         and task_id in ('03750d1c-64ca-11f1-9ed2-1e98f210beb6',
                                                         '483311ea-452c-11f1-904e-82a496b47771',
                                                         'c5f49480-a089-11f1-99a7-baab7fd18eef',
                                                         '4896a5da-96e7-11f1-b60d-1a7135ad3f79',
                                                         'b59027cb-29ec-11f1-89db-72159489a3ba',
                                                         '8fb88451-b81e-11f1-9f82-7a5058d00d8b',
                                                         '2584bef5-b810-11f1-9f82-7a5058d00d8b',
                                                         '51f33653-b8de-11f1-9f82-7a5058d00d8b',
                                                         '18eb641a-3735-11f1-afb3-0e18d6f21d86',
                                                         'c5c04be8-60ca-11f1-8a6d-ee4781d0c6d3',
                                                         '2cdffaee-7c75-11f1-9e96-323a523aa895',
                                                         '266cbb53-a83c-11f1-88bc-9a774b49999a',
                                                         '76462a68-739f-11f1-a09c-bac65e7843bf',
                                                         'dd5f9dc1-9faf-11f1-8431-36040a8699dc',
                                                         '98389277-a6bd-11f1-a50f-923a7fe332ab',
                                                         'd33384b4-97f0-11f1-8a71-aaa54a17b8cd',
                                                         '6c4e74df-75fe-11f1-8507-9e30379135fa',
                                                         '646b9d3f-a845-11f1-84f8-0e913e85d617',
                                                         '7b964ac1-8cf1-11f1-9d2b-26028f7bed64',
                                                         'ff7f432a-ac4f-11f1-84f8-0e913e85d617',
                                                         'd515f2e3-a1f9-11f1-8431-36040a8699dc',
                                                         '44203f8f-8cd6-11f1-8c72-6a3f84247d75',
                                                         '761ec68f-85e1-11f1-8902-f6f678730791',
                                                         'caa25e0d-8f20-11f1-97d8-7e510aadfe06',
                                                         '6310a4e2-a057-11f1-a5ac-c6f6fd5c25a6',
                                                         '6e930e56-964f-11f1-aefb-7a3499698900',
                                                         '7b59b818-9c9a-11f1-8193-16b297413dd9',
                                                         '7ba4961e-a5ea-11f1-ac28-c66b87828f37',
                                                         '9eff754f-b02b-11f1-b31a-aa1fef4be8a0',
                                                         'f5f11830-ac2d-11f1-9622-4e432f9a1dcb',
                                                         '9f5fa778-b280-11f1-84f8-523cd905cd3c',
                                                         '3f54360f-adc2-11f1-921f-9ebd7b9d4d06',
                                                         '26d45439-a852-11f1-84f8-0e913e85d617',
                                                         'ce798310-b5af-11f1-b5e2-3ebafa561750',
                                                         '1a341645-b6ab-11f1-a989-e24ff73a1ae7',
                                                         'f1267a5c-b367-11f1-9dfa-ba930d87a765',
                                                         'fb93d3a4-adb9-11f1-a39a-aab4919d977f');


-- 2. Backup the tasks that will be updated (should contain 37 rows)
create table cft_task_db.rwa_role_category_admin_bakcup as
select * from cft_task_db.tasks
where tasks.jurisdiction='IA'
  and role_category = 'CTSC'
  and state = 'UNASSIGNED'
  and task_type = 'detainedListTheCase'
  and task_id in ('03750d1c-64ca-11f1-9ed2-1e98f210beb6',
                  '483311ea-452c-11f1-904e-82a496b47771',
                  'c5f49480-a089-11f1-99a7-baab7fd18eef',
                  '4896a5da-96e7-11f1-b60d-1a7135ad3f79',
                  'b59027cb-29ec-11f1-89db-72159489a3ba',
                  '8fb88451-b81e-11f1-9f82-7a5058d00d8b',
                  '2584bef5-b810-11f1-9f82-7a5058d00d8b',
                  '51f33653-b8de-11f1-9f82-7a5058d00d8b',
                  '18eb641a-3735-11f1-afb3-0e18d6f21d86',
                  'c5c04be8-60ca-11f1-8a6d-ee4781d0c6d3',
                  '2cdffaee-7c75-11f1-9e96-323a523aa895',
                  '266cbb53-a83c-11f1-88bc-9a774b49999a',
                  '76462a68-739f-11f1-a09c-bac65e7843bf',
                  'dd5f9dc1-9faf-11f1-8431-36040a8699dc',
                  '98389277-a6bd-11f1-a50f-923a7fe332ab',
                  'd33384b4-97f0-11f1-8a71-aaa54a17b8cd',
                  '6c4e74df-75fe-11f1-8507-9e30379135fa',
                  '646b9d3f-a845-11f1-84f8-0e913e85d617',
                  '7b964ac1-8cf1-11f1-9d2b-26028f7bed64',
                  'ff7f432a-ac4f-11f1-84f8-0e913e85d617',
                  'd515f2e3-a1f9-11f1-8431-36040a8699dc',
                  '44203f8f-8cd6-11f1-8c72-6a3f84247d75',
                  '761ec68f-85e1-11f1-8902-f6f678730791',
                  'caa25e0d-8f20-11f1-97d8-7e510aadfe06',
                  '6310a4e2-a057-11f1-a5ac-c6f6fd5c25a6',
                  '6e930e56-964f-11f1-aefb-7a3499698900',
                  '7b59b818-9c9a-11f1-8193-16b297413dd9',
                  '7ba4961e-a5ea-11f1-ac28-c66b87828f37',
                  '9eff754f-b02b-11f1-b31a-aa1fef4be8a0',
                  'f5f11830-ac2d-11f1-9622-4e432f9a1dcb',
                  '9f5fa778-b280-11f1-84f8-523cd905cd3c',
                  '3f54360f-adc2-11f1-921f-9ebd7b9d4d06',
                  '26d45439-a852-11f1-84f8-0e913e85d617',
                  'ce798310-b5af-11f1-b5e2-3ebafa561750',
                  '1a341645-b6ab-11f1-a989-e24ff73a1ae7',
                  'f1267a5c-b367-11f1-9dfa-ba930d87a765',
                  'fb93d3a4-adb9-11f1-a39a-aab4919d977f');

-- 3. Verify the backup has the expected 37 rows
select count(*) from cft_task_db.rwa_role_category_admin_bakcup;

-- 4. Update role_category='ADMIN' for selected tasks
update cft_task_db.tasks set role_category='ADMIN'
where tasks.jurisdiction='IA'
  and role_category = 'CTSC'
  and state = 'UNASSIGNED'
  and task_type = 'detainedListTheCase'
  and task_id in ('03750d1c-64ca-11f1-9ed2-1e98f210beb6',
                  '483311ea-452c-11f1-904e-82a496b47771',
                  'c5f49480-a089-11f1-99a7-baab7fd18eef',
                  '4896a5da-96e7-11f1-b60d-1a7135ad3f79',
                  'b59027cb-29ec-11f1-89db-72159489a3ba',
                  '8fb88451-b81e-11f1-9f82-7a5058d00d8b',
                  '2584bef5-b810-11f1-9f82-7a5058d00d8b',
                  '51f33653-b8de-11f1-9f82-7a5058d00d8b',
                  '18eb641a-3735-11f1-afb3-0e18d6f21d86',
                  'c5c04be8-60ca-11f1-8a6d-ee4781d0c6d3',
                  '2cdffaee-7c75-11f1-9e96-323a523aa895',
                  '266cbb53-a83c-11f1-88bc-9a774b49999a',
                  '76462a68-739f-11f1-a09c-bac65e7843bf',
                  'dd5f9dc1-9faf-11f1-8431-36040a8699dc',
                  '98389277-a6bd-11f1-a50f-923a7fe332ab',
                  'd33384b4-97f0-11f1-8a71-aaa54a17b8cd',
                  '6c4e74df-75fe-11f1-8507-9e30379135fa',
                  '646b9d3f-a845-11f1-84f8-0e913e85d617',
                  '7b964ac1-8cf1-11f1-9d2b-26028f7bed64',
                  'ff7f432a-ac4f-11f1-84f8-0e913e85d617',
                  'd515f2e3-a1f9-11f1-8431-36040a8699dc',
                  '44203f8f-8cd6-11f1-8c72-6a3f84247d75',
                  '761ec68f-85e1-11f1-8902-f6f678730791',
                  'caa25e0d-8f20-11f1-97d8-7e510aadfe06',
                  '6310a4e2-a057-11f1-a5ac-c6f6fd5c25a6',
                  '6e930e56-964f-11f1-aefb-7a3499698900',
                  '7b59b818-9c9a-11f1-8193-16b297413dd9',
                  '7ba4961e-a5ea-11f1-ac28-c66b87828f37',
                  '9eff754f-b02b-11f1-b31a-aa1fef4be8a0',
                  'f5f11830-ac2d-11f1-9622-4e432f9a1dcb',
                  '9f5fa778-b280-11f1-84f8-523cd905cd3c',
                  '3f54360f-adc2-11f1-921f-9ebd7b9d4d06',
                  '26d45439-a852-11f1-84f8-0e913e85d617',
                  'ce798310-b5af-11f1-b5e2-3ebafa561750',
                  '1a341645-b6ab-11f1-a989-e24ff73a1ae7',
                  'f1267a5c-b367-11f1-9dfa-ba930d87a765',
                  'fb93d3a4-adb9-11f1-a39a-aab4919d977f');

-- 5. Run the count from step one should return 0

-- 6. Drop the temp backup table if not needed
drop table if exists cft_task_db.rwa_role_category_admin_bakcup
