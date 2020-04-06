CREATE TABLE `action_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `template_id` varchar(255) DEFAULT NULL,
  `action_type` varchar(255) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `data` blob DEFAULT NULL,
  `created` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `callback_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `provider` varchar(255) NOT NULL,
  `type` varchar(45) NOT NULL,
  `translation_template_type` varchar(255) DEFAULT NULL,
  `id_path` varchar(255) DEFAULT NULL,
  `template` blob DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `updated` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_provider_translation_template_type` (`provider`,`translation_template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `providers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `provider_id` varchar(255) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `use_case` varchar(255) DEFAULT NULL,
  `provider_name` varchar(255) DEFAULT NULL,
  `partitions` bigint(20) DEFAULT NULL,
  `created` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_provider_id_use_case` (`provider_id`,`use_case`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `state_transitions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transition_id` varchar(255) DEFAULT NULL,
  `workflow_template_id` varchar(255) DEFAULT NULL,
  `from_state` varchar(255) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `data` blob DEFAULT NULL,
  `created` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_transition_id` (`transition_id`),
  KEY `idx_workflow_template_id_from_state` (`workflow_template_id`,`from_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `workflow_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `template_id` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `rules` blob DEFAULT NULL,
  `start_state` blob DEFAULT NULL,
  `created` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `workflow_instances` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `partition_id` int(11) NOT NULL DEFAULT 1,
  `workflow_id` varchar(255) DEFAULT NULL,
  `template_id` varchar(255) DEFAULT NULL,
  `current_state` varchar(255) DEFAULT NULL,
  `completed` bit(1) DEFAULT NULL,
  `data` blob DEFAULT NULL,
  `created` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated` datetime(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`,`partition_id`),
  UNIQUE KEY `uniq_workflow_id_partition_id` (`workflow_id`,`partition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY RANGE (`partition_id`)
(PARTITION `p0` VALUES LESS THAN (1) ENGINE = InnoDB,
 PARTITION `p1` VALUES LESS THAN (2) ENGINE = InnoDB,
 PARTITION `p2` VALUES LESS THAN (3) ENGINE = InnoDB,
 PARTITION `p3` VALUES LESS THAN (4) ENGINE = InnoDB,
 PARTITION `p4` VALUES LESS THAN (5) ENGINE = InnoDB,
 PARTITION `p5` VALUES LESS THAN (6) ENGINE = InnoDB,
 PARTITION `p6` VALUES LESS THAN (7) ENGINE = InnoDB,
 PARTITION `p7` VALUES LESS THAN (8) ENGINE = InnoDB,
 PARTITION `p8` VALUES LESS THAN (9) ENGINE = InnoDB,
 PARTITION `p9` VALUES LESS THAN (10) ENGINE = InnoDB,
 PARTITION `p10` VALUES LESS THAN (11) ENGINE = InnoDB,
 PARTITION `p11` VALUES LESS THAN (12) ENGINE = InnoDB,
 PARTITION `p12` VALUES LESS THAN (13) ENGINE = InnoDB,
 PARTITION `p13` VALUES LESS THAN (14) ENGINE = InnoDB,
 PARTITION `p14` VALUES LESS THAN (15) ENGINE = InnoDB,
 PARTITION `p15` VALUES LESS THAN (16) ENGINE = InnoDB,
 PARTITION `p16` VALUES LESS THAN (17) ENGINE = InnoDB,
 PARTITION `p17` VALUES LESS THAN (18) ENGINE = InnoDB,
 PARTITION `p18` VALUES LESS THAN (19) ENGINE = InnoDB,
 PARTITION `p19` VALUES LESS THAN (20) ENGINE = InnoDB,
 PARTITION `p20` VALUES LESS THAN (21) ENGINE = InnoDB,
 PARTITION `p21` VALUES LESS THAN (22) ENGINE = InnoDB,
 PARTITION `p22` VALUES LESS THAN (23) ENGINE = InnoDB,
 PARTITION `p23` VALUES LESS THAN (24) ENGINE = InnoDB,
 PARTITION `p24` VALUES LESS THAN (25) ENGINE = InnoDB,
 PARTITION `p25` VALUES LESS THAN (26) ENGINE = InnoDB,
 PARTITION `p26` VALUES LESS THAN (27) ENGINE = InnoDB,
 PARTITION `p27` VALUES LESS THAN (28) ENGINE = InnoDB,
 PARTITION `p28` VALUES LESS THAN (29) ENGINE = InnoDB,
 PARTITION `p29` VALUES LESS THAN (30) ENGINE = InnoDB,
 PARTITION `p30` VALUES LESS THAN (31) ENGINE = InnoDB,
 PARTITION `p31` VALUES LESS THAN (32) ENGINE = InnoDB,
 PARTITION `p32` VALUES LESS THAN (33) ENGINE = InnoDB,
 PARTITION `p33` VALUES LESS THAN (34) ENGINE = InnoDB,
 PARTITION `p34` VALUES LESS THAN (35) ENGINE = InnoDB,
 PARTITION `p35` VALUES LESS THAN (36) ENGINE = InnoDB,
 PARTITION `p36` VALUES LESS THAN (37) ENGINE = InnoDB,
 PARTITION `p37` VALUES LESS THAN (38) ENGINE = InnoDB,
 PARTITION `p38` VALUES LESS THAN (39) ENGINE = InnoDB,
 PARTITION `p39` VALUES LESS THAN (40) ENGINE = InnoDB,
 PARTITION `p40` VALUES LESS THAN (41) ENGINE = InnoDB,
 PARTITION `p41` VALUES LESS THAN (42) ENGINE = InnoDB,
 PARTITION `p42` VALUES LESS THAN (43) ENGINE = InnoDB,
 PARTITION `p43` VALUES LESS THAN (44) ENGINE = InnoDB,
 PARTITION `p44` VALUES LESS THAN (45) ENGINE = InnoDB,
 PARTITION `p45` VALUES LESS THAN (46) ENGINE = InnoDB,
 PARTITION `p46` VALUES LESS THAN (47) ENGINE = InnoDB,
 PARTITION `p47` VALUES LESS THAN (48) ENGINE = InnoDB,
 PARTITION `p48` VALUES LESS THAN (49) ENGINE = InnoDB,
 PARTITION `p49` VALUES LESS THAN (50) ENGINE = InnoDB,
 PARTITION `p50` VALUES LESS THAN (51) ENGINE = InnoDB,
 PARTITION `p51` VALUES LESS THAN (52) ENGINE = InnoDB,
 PARTITION `p52` VALUES LESS THAN (53) ENGINE = InnoDB,
 PARTITION `p53` VALUES LESS THAN (54) ENGINE = InnoDB,
 PARTITION `p54` VALUES LESS THAN MAXVALUE ENGINE = InnoDB);
