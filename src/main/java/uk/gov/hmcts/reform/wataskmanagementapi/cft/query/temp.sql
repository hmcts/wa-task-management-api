EXPLAIN (analyze, verbose)
SELECT count(DISTINCT taskresour0_.task_id)
FROM cft_task_db.tasks taskresour0_
       INNER JOIN cft_task_db.task_roles taskrolere1_ ON
  taskresour0_.task_id = taskrolere1_.task_id
WHERE taskresour0_.jurisdiction = 'IA'
  AND (taskresour0_.state IN ('ASSIGNED', 'UNASSIGNED'))
  AND 1 = 1
  AND 1 = 1
  AND 1 = 1
  AND 1 = 1
  AND taskresour0_.role_category = 'LEGAL_OPERATIONS'
  AND (taskrolere1_.role_name = 'hmcts-legal-operations' AND (taskresour0_.security_classification IN (
                                                                                                       'PRIVATE',
                                                                                                       'PUBLIC')) AND (
         taskrolere1_.authorizations = '{}') AND 1 = 1 AND 1 = 1 AND 1 = 1 AND 1 = 1 AND 1 = 1
  OR taskrolere1_.role_name = 'case-manager' AND taskresour0_.security_classification = 'PUBLIC'
         AND (taskrolere1_.authorizations = '{}')
         AND taskresour0_.jurisdiction = 'IA' AND 1 = 1 AND 1 = 1 AND
     taskresour0_.case_type_id = 'Asylum' AND (taskresour0_.case_id IN ('1648824275071941',
                                                                        '1649070261515249', '1649071562813135',
                                                                        '1648824194842828', '1649071633264425',
                                                                        '1649070919067361', '1649070134588746',
                                                                        '1649071337496083', '1649071653545554',
                                                                        '1649070328922644', '1649069868635053',
                                                                        '1649070418912638', '1649071154555482',
                                                                        '1649070866554882', '1649071249688579',
                                                                        '1649071462517828', '1649071052257934',
                                                                        '1649070382420113', '1649069999145733',
                                                                        '1649069581675766', '1649069581675766',
                                                                        '1649069581675766', '1649069581675766',
                                                                        '1649069581675766', '1649069581675766',
                                                                        '1649069581675766', '1649069581675766',
                                                                        '1649069581675766', '1649069581675766',
                                                                        '1649069581675766', '1649069581675766',
                                                                        '1649069581675766'))
  OR (taskrolere1_.role_name = 'tribunal-caseworker' AND taskresour0_.security_classification = 'PUBLIC' AND
      (taskrolere1_.authorizations = '{}') AND
      taskresour0_.jurisdiction = 'IA' AND 1 = 1 AND 1 = 1 AND 1 = 1 AND 1 = 1
    OR taskrolere1_.role_name = 'case-allocator' AND taskresour0_.security_classification = 'PUBLIC'
        AND (taskrolere1_.authorizations = '{}')
        AND taskresour0_.jurisdiction = 'IA' AND 1 = 1 AND 1 = 1 AND 1 = 1 AND 1 = 1
    OR taskrolere1_.role_name = 'task-supervisor' AND
       taskresour0_.security_classification = 'PUBLIC' AND (
         taskrolere1_.authorizations = '{}') AND taskresour0_.jurisdiction = 'IA' AND 1 = 1 AND 1 = 1 AND 1 = 1
        AND 1 = 1) AND 1 = 1)
  AND taskrolere1_.READ = true limit 25;
