{
	"type": "A",
	"loadNotFinishedState": true,
	"loadNotFinishedStateDurationMin": -60,
	"channelManagerConfig": {
		"oldStateCheckHandlerConfig": {
			"usePreviousProcessInfo": false,
			"statusSuccess": "00",
			"statusIng"    : "01",
			"statusFail"   : "99",
			"nodeMap": {
				"S"   : 10, "SNDR": 10, "SEND": 10,
				"H"   : 20, "B"   : 20, "BRKR": 20, "SBRK": 20,
				"Q"   : 30, "E"   : 30, "REPL": 30, "RQST": 30,
				"RBRK": 40,
				"R"   : 50, "RECV": 50, "RCVR": 50,
				"SNRC": 60
			}
		},
		"channelConfigs": [
			{
				"name": "RestChannel",
				"threadCount": 4,
				"type": 2,
				"hostName": "localhost",
				"qmgrName": "",
				"waitTime":1000,
				"port": 0,
				"userId": "none",
				"password": "none",
				"channelName": "none",
				"queueName": "none",
				"module": "n",
				"ccsid": 0,
				"characterSet": 0,
				"autoCommit": false,
				"bindMode": false,
				"maxCommitWait": 100,
				"delayForNoMessage": 1000,
				"commitCount": 100,
				"maxCacheSize":100000, 
				"delayForMaxCache":300,
				"cacheIndex": [0,1,2,3],
				"healthCheck": false,
				"disable":false
			},
			{
				"name": "MQChannel",
				"threadCount": 4,
				"type": 0,
				"hostName": "%hostName%",
				"qmgrName": "%qmgrName%",
				"port": %port%,
				"userId": "%userId%",
				"password": "%password%",
				"channelName": "%channelName%",
				"queueName": "%queueName%",
				"waitTime":1000,
				"module": "w",
				"ccsid": 1208,
				"characterSet": 1208,
				"autoCommit": false,
				"bindMode": false,
				"maxCommitWait": 100,
				"delayForNoMessage": 1000,
				"commitCount": 100,
				"maxCacheSize":100000, 
				"delayForMaxCache":300,				
				"cacheIndex": [0,1,2,3],
				"healthCheck": true,
				"disable": %wmqDisable%
			},{
				"name": "ILinkChannel",
				"threadCount": 4,
				"type": 0,
				"hostName": "%hostName%",
				"qmgrName": "%qmgrName%",
				"port": %port%,
				"userId": "%userId%",
				"password": "%password%",
				"channelName": "%channelName%",
				"queueName": "%queueName%",
				"waitTime":1000,
				"module": "i",
				"ccsid": 1208,
				"characterSet": 1208,
				"autoCommit": false,
				"bindMode": false,
				"maxCommitWait": 100,
				"delayForNoMessage": 1000,
				"commitCount": 100,
				"maxCacheSize":100000,
				"delayForMaxCache":300,
				"cacheIndex": [0,1,2,3],        
				"healthCheck": true,
				"disable": %iLinkDisable%
		    }
		],
		"delayOnException" : 5000,
		"translateMsgOnException" : false
	},
	"cacheManagerConfig": {
		"vendor": "infinispan",
		"diskPath": "caches",
		"persistant": true,
		"checkSizeDelay": 1000,
		"distributeCacheConfigs": [
			{
				"name": "dc01",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "dc02",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "dc03",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "dc04",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			}
		],
		"mergeCacheConfig": {
			"name": "mc01",
			"memoryUnit": 3,
			"heapSize": 10000,
			"diskSize": 1,
			"maxEntries": 100000
		},
		"backupCacheConfig": {
			"name": "backupCache",
			"memoryUnit": 3,
			"heapSize": 10000,
			"diskSize": 1,
			"maxEntries": 100000
		},
		"botCacheConfigs": [
			{
				"name": "bc01",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "bc02",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "bc03",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "bc04",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			}
		],
		"cloneCacheConfigs": [
			{
				"name": "cc01",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "cc02",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			},
			{
				"name": "cc03",
				"memoryUnit": 3,
				"heapSize": 10000,
				"diskSize": 1,
				"maxEntries": 100000
			}
		],
		"finCacheConfig": {
			"name": "fc01",
			"memoryUnit": 3,
			"heapSize": 10000000,
			"diskSize": 1,
			"maxEntries": 100000000
		},
		"routingCacheConfig": {
			"name": "rc01",
			"memoryUnit": 2,
			"heapSize": 10000,
			"diskSize": 100,
			"maxEntries": 100000
		},
		"errorCache01Config": {
			"name": "ec01",
			"memoryUnit": 3,
			"heapSize": 10000,
			"diskSize": 1,
			"maxEntries": 100000
		},
		"errorCache02Config": {
			"name": "ec02",
			"memoryUnit": 3,
			"heapSize": 10000,
			"diskSize": 1,
			"maxEntries": 100000
		},
		"testCacheConfig": null,
		"interfaceCacheConfig": {
			"name": "ic01",
			"memoryUnit": 3,
			"heapSize": 1000000,
			"diskSize": 5,
			"maxEntries": 10000000
		},
		"unmatchCacheConfig": {
			"name": "uc01",
			"memoryUnit": 3,
			"heapSize": 1000000,
			"diskSize": 5,
			"maxEntries": 10000000
		},
		"systemErrorCacheConfig": {
			"name": "sec01",
			"memoryUnit": 3,
			"heapSize": 10000,
			"diskSize": 5,
			"maxEntries": 100000
		}
	},
	"serverManagerConfig": {
		"name": "t9",
		"version": "1.0.0",
		"site": "mapo",
		"startOnBoot": true
	},
	"loaderManagerConfig": {
		"threadCount": 1,
		"commitCount": 1000,
		"delayForNoMessage": 1000,
		"loadError": true,
		"loadContents": false,
		"name": "loader"
	},
	"boterManagerConfig": {
		"threadCount": 1,
		"commitCount": 1000,
		"delayForNoMessage": 1000,
		"maxRoutingCacheSize": 10000,
		"name": "boter"
	},
	"botLoaderManagerConfig": {
		"threadCount": 1,
		"commitCount": 1000,
		"delayForNoMessage": 1000,
		"name": "botLoader"
	},
	"finisherManagerConfig": {
		"threadCount": 1,
		"commitCount": 100,
		"delayForNoMessage": 1000,
		"useWaitForCleaning": true,
		"waitForFinishedCleaningSec": 1,
		"waitForCleaningSec": 600,
		"delayForDoCleaning": 10000, 
		"resetWhenStart": false,
		"name": "finisher"
	},
	"interfaceCacheManagerConfig": {
		"refreshDelay": 3600
	},
	"traceErrorHandlerManagerConfig": {
		"name": "traceErrorHandler",
		"delayForNoMessage": 10000,
		"maxRetry": 3, 
		"exceptionDelay": 5000
	},
	"botErrorHandlerManagerConfig": {
		"name": "traceErrorHandler",
		"delayForNoMessage": 10000,
		"maxRetry": 3, 
		"exceptionDelay": 5000
	},
	"unmatchHandlerManagerConfig": {
		"name": "unmatchHandler",
		"delayForNoMessage": 1000,
		"delayForDoChecking": 10000, 
		"exceptionDelay": 100
	},
	"policyConfig": {
		"name": "policyConfig",
		"policy": 100,
		"policyCheckDelay": 1000,
		"databaseCheckDelay": 10000,
		"exceptionDelay": 1000,
		"policyCount": 2,
		"disable": true		
	}, 
	"testerManagerConfig": {
		"name": "generateMsgTester",
		"exceptionDelay": 5000,
		"repeatDelaySec":  300,
		"paramsList": [
			{
				"port": %port%,
				"hostName": "%hostName%",
				"qmgrName": "%qmgrName%",				
				"channelName": "%channelName%",
				"queueName": "%queueName%",
			  	"module":"w",
			  	"generateCount":"10000",
			  	"commitCount":"1000",
			  	"traceMsgCreator":"rose.mary.trace.simulator.DefaultTraceMsgCreator",
			  	"data": "한글1234567890qwertyuiop"
			},{
				"port": %port%,
				"hostName": "%hostName%",
				"qmgrName": "%qmgrName%",				
				"channelName": "%channelName%",
				"queueName": "%queueName%",
			  	"module":"i",
			  	"generateCount":"10000",
			  	"commitCount":"1000",
			  	"traceMsgCreator":"rose.mary.trace.simulator.DefaultTraceMsgCreator",
			  	"data": "한글1234567890qwertyuiop"
			}
			
		]
	},
	"systemErrorTestManagerConfig": {
		"name": "systemErrorTester",
		"startOnLoad" : false,
		"delay": 10000, 
		"exceptionDelay": 5000
	}	
}
