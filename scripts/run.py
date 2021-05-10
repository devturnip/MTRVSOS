import os
import subprocess
import threading
import re
import time
import json

from datetime import datetime
from pathlib import Path
from random import randrange

from elasticsearch import Elasticsearch
from elasticsearch_dsl import Search

class runner (threading.Thread):
    def __init__(self, name, method):
        threading.Thread.__init__(self)
        self.name = name
        self.method = method
        
    def run(self):
        print("Starting Thread " + self.name)
        executeJarRunner(self.method)
        print("Exiting Thread "+ self.name)

def parseException(file):
    with open(file) as f:
        text = f.read()
    exceptions = re.findall(r'(?m)^.*?Exception.*(?:\n+^\s*at .*)+', text, re.M)
    # for exception in exceptions:
    #     print(exception)
    
    exceptions[:] = [e for e in exceptions if "MTPException" not in e]
    exceptions[:] = [e for e in exceptions if "InterruptedException" not in e]
        
    # for num, e in enumerate(exceptions):
    #     if any("MTPException" in e for e in exceptions):
    #         exceptions.pop(num)
    #     elif any("InterruptedException" in e for e in exceptions):
    #         exceptions.pop(num)
    return exceptions   

def getJarFile():
    base = Path(__file__).parent.parent / "target"
    for file in os.listdir(base):
        if "shaded" in file:
            return base / file

def executeJarRunner(mr):
    ret, dt_string, ipaddr, fname, var_dict = executeJar(mr)
    if ret != 0:
        print("failed with return code != 0")
    else:
        if ("MRConsistentPowerRegulation" in mr):
            checkMRConsistentPowerRegulation(index=dt_string, host=ipaddr, fname=fname, var_dict=var_dict)
        elif ("MRConsistentReliabilityThreshold" in mr):
            checkMRConsistentReliabilityThreshold(index=dt_string, host=ipaddr, fname=fname, var_dict=var_dict)
        
def randomiseVariables(size="r.small"):
    #1 sh smarthome = 1000 units
    #control scenarios
    if ("r.small" in size):
        #small cities
        argument_dict = {
            "pg" : randrange(1,2),
            "pd" : randrange(1,2),
            "sh" : randrange(1,10),
            "ev" : randrange(1,10)
        }
    elif ("r.medium" in size):
        #medium cities
        argument_dict = {
            "pg" : randrange(3,5),
            "pd" : randrange(3,5),
            "sh" : randrange(30,50),
            "ev" : randrange(10,20)
        }
    elif ("r.large" in size):
        #large cities
        argument_dict = {
            "pg" : randrange(5,10),
            "pd" : randrange(5,10),
            "sh" : randrange(50,100),
            "ev" : randrange(20,50)
        }
    elif ("r.original" in size):
        #no control
        argument_dict = {
            "pg" : randrange(1,100),
            "pd" : randrange(1,100),
            "sh" : randrange(1,100),
            "ev" : randrange(1,100)
        }
    
    # argument_list.append(pg,pd,sh,ev)
    print(argument_dict)
    return argument_dict

def executeJar(mr):
    var_dict = randomiseVariables("r.large")
    #flags
    #test flags for inducing failure
    # powergen = '-p'
    # pg = '1'
    # powerdi = '-pd'
    # pd = '2'
    # smarthome = '-sh'
    # sh = '150'
    # electronicveh = '-ev'
    # ev = '5'
    
    powergen = '-p'
    pg = str(var_dict["pg"])
    powerdi = '-pd'
    pd = str(var_dict["pd"])
    smarthome = '-sh'
    sh = str(var_dict["sh"])
    electronicveh = '-ev'
    ev = str(var_dict["ev"])
    
    runtime = '-rt'
    rt = '90'
    wait = '-wt'
    wt = '6000'
    
    indexName = '-indexname'
    now = datetime.now()
    dt_string = now.strftime("smartgrid-%Y-%m-%dt%H.%M.%S.%f")
    # indexName = {'-indexname' : dt_string}
    
    elastichost = '-elastichost'
    #ipaddr = '192.168.0.31' #self
    ipaddr = '192.168.25.19' #lab
    
    jarfile = getJarFile()
    
    if ("MRConsistentReliabilityThreshold" in mr):
        testrun = '-testrun'
        testname = 'consistentreliabilitythreshold'
        fname = dt_string + "err.txt"
        f = open(fname, "w")
        ret = subprocess.call(['java', '-jar', jarfile, powergen, pg, powerdi, pd, smarthome, sh, electronicveh, ev, runtime, rt, wait, wt, indexName, dt_string, testrun, testname, elastichost, ipaddr],  stderr=f)
        
    else :
        fname = dt_string + "err.txt"
        f = open(fname, "w")
        ret = subprocess.call(['java', '-jar', jarfile, powergen, pg, powerdi, pd, smarthome, sh, electronicveh, ev, runtime, rt, wait, wt, indexName, dt_string, elastichost, ipaddr], stderr=f)
    
    return ret, dt_string, ipaddr, fname, var_dict

def queryES(index, host):
    host_addr = 'http://' + host + ':9200/'
    client = Elasticsearch([host_addr])
    s = Search(using=client, index=index)
    request = s.source(['hash', 'author_date', 'author'])
        
    response = s.scan()
    return response   

def queryESALL(index, host):
    host_addr = 'http://' + host + ':9200/'
    client = Elasticsearch([host_addr])

    # Build a DSL Search object on the 'commits' index, 'summary' document type
    request = Search(using=client, index=index, doc_type='summary')

    # Run the Search, using the scan interface to get all resuls
    response = request.scan()
    for commit in response:
        print(commit)
        
def writeToES(host, indexname, msgBody):
    host_addr = 'http://' + host + ':9200/'
    client = Elasticsearch([host_addr])

    # create an index in elasticsearch, ignore status code 400 (index already exists)
    client.indices.create(index=indexname, ignore=400)

    # datetimes will be serialized
    res = client.index(index=indexname, body=msgBody)
    return res

def checkMRConsistentReliabilityThreshold(index, host, fname, var_dict):
    
    msgBody = {
        "timestamp" : "",
        "mr" : "",
        "test_type" : "r.original",
        "result" : "",
        "exception_body" : "",
        "test_indexname" : "",
        "cs_arguments" : json.dumps(var_dict)
    }
    
    
    exceptions = parseException(fname)
    if exceptions:
        print("MRConsistentReliabilityThreshold --", index, "-- result:", "FAILED DUE TO EXCEPTIONS")
        for e in exceptions:
            print(e)
            print("LINE")
        
        strExcept = str1 = ''.join(exceptions)
        
        msgBody["timestamp"] = datetime.now().replace(microsecond=0).isoformat()
        msgBody["mr"] = "MRConsistentReliabilityThreshold"
        msgBody["result"] = "FAILED_DUE_TO_EXCEPTIONS"
        msgBody["exception_body"] = strExcept
        msgBody["test_indexname"] = index
        
        res = writeToES(host=host, indexname="experiment_results", msgBody=msgBody)
        print(res)
        
    else:
        response_init = queryES(index, host)
        response = queryES(index, host)
        count = 0
        sh = []
        sh_interrupted = []
        sh_interrupted_5t = []
        pg_init_timestamps = []
        min_count = 0;
        #obtain first instance of power init
        for hit in response_init:
            values = hit.to_dict()
            if 'agent_type' in values:
                agent_type = values['agent_type']
                if 'PowerGenAgent' in agent_type:
                            if 'action' in values:
                                action = values['action']
                                if 'power_generation.init' in action:
                                    pg_init_timestamps.append(values['timestamp'])
                                    
        
        time_to_compare = datetime.strptime(pg_init_timestamps[-1], '%Y-%m-%dT%H:%M:%S.%fZ')
        print("init: ",time_to_compare)
        
        for hit in response:
            values = hit.to_dict()
            if 'agent_type' in values:
                agent_type = values['agent_type']
                agent_time = datetime.strptime(values['timestamp'], '%Y-%m-%dT%H:%M:%S.%fZ')
                if 'SmartHomeAgent' in agent_type:
                        count += 1
                        sh.append(values['agent_name'])
                        
                if agent_time.timestamp() > time_to_compare.timestamp():
                    if 'SmartHomeAgent' in agent_type:
                        if 'receive_power' in values:
                            received = values['receive_power']
                            if 'false' in received:
                                print(datetime.strptime(values['timestamp'], '%Y-%m-%dT%H:%M:%S.%fZ'), values['agent_name'])
                                sh_interrupted.append(values['agent_name'])
                                min_count += 1;
                                #print(values)
                        elif 'request_power' in values:
                            request = values['request_power']
                            if 'no_reply_5t' in request:
                                print(datetime.strptime(values['timestamp'], '%Y-%m-%dT%H:%M:%S.%fZ'), values['agent_name'])
                                sh_interrupted_5t.append(values['agent_name'])
                                min_count += 1;
        
        
        print(len(set(sh)), "total customers")
        print(len(set(sh_interrupted)), "interrupted customers:", set(sh_interrupted))
        print(len(set(sh_interrupted_5t)), "interrupted customers 5t:", set(sh_interrupted_5t))
        total = sh_interrupted + sh_interrupted_5t
        print(len(set(total)), "interrupted customers total:", set(total))
        print("total interrupted minutes:", min_count)
        
        # saifi = sum(num of customers interrupted) / total num of customers served
        saifi = len(set(total)) / len(set(sh))
        print("SAIFI:", saifi)
        # asai = 1 - (sum(interruption_time * num of customers interrupted) / (total customers * time period under study)) * 100
        asai = (1-(min_count * len(set(total))) / (len(set(sh))*90)) * 100 
        print("ASAI:", asai)
        
        msgBody["timestamp"] = datetime.now().replace(microsecond=0).isoformat()
        msgBody["mr"] = "MRConsistentPowerRegulation"
        msgBody["result"] = "PASSED"
        msgBody["exception_body"] = "none"
        msgBody["test_indexname"] = index
    
        
def checkMRConsistentPowerRegulation(index, host, fname, var_dict):
    
    msgBody = {
        "timestamp" : "",
        "mr" : "",
        "test_type" : "r.original",
        "result" : "",
        "exception_body" : "",
        "test_indexname" : "",
        "cs_arguments" : json.dumps(var_dict)
    }
    
    
    exceptions = parseException(fname)
    if exceptions:
        print("MRConsistentPowerRegulation --", index, "-- result:", "FAILED DUE TO EXCEPTIONS")
        for e in exceptions:
            print(e)
            print("LINE")
        
        strExcept = str1 = ''.join(exceptions)
        
        msgBody["timestamp"] = datetime.now().replace(microsecond=0).isoformat()
        msgBody["mr"] = "MRConsistentPowerRegulation"
        msgBody["result"] = "FAILED_DUE_TO_EXCEPTIONS"
        msgBody["exception_body"] = strExcept
        msgBody["test_indexname"] = index
        
        res = writeToES(host=host, indexname="experiment_results", msgBody=msgBody)
        print(res)
        
    else :  
        response = queryES(index, host)
        returnValue = False;
        for hit in response:
            values = hit.to_dict()
            if 'SoSAgent' in values.values():
                if 'action' in values:
                    action = values['action']
                    if action == 'sosagent.regulate_power':
                        if values['regulate_power'] == 'true':
                            returnValue = True
                            break
        print("MRConsistentPowerRegulation --", index, "-- result:", returnValue)
        
        if (returnValue == True):
            msgBody["timestamp"] = datetime.now().replace(microsecond=0).isoformat()
            msgBody["mr"] = "MRConsistentPowerRegulation"
            msgBody["result"] = "PASSED"
            msgBody["exception_body"] = "none"
            msgBody["test_indexname"] = index
        else:
            msgBody["timestamp"] = datetime.now().replace(microsecond=0).isoformat()
            msgBody["mr"] = "MRConsistentPowerRegulation"
            msgBody["result"] = "FAILED"
            msgBody["exception_body"] = "none"
            msgBody["test_indexname"] = index
        
        res = writeToES(host=host, indexname="experiment_results", msgBody=msgBody)
        print(res)
    

def runTests(times, instances, testname):
    for x in range(times):
        thread_list = []
        for x in range(instances):
            thread = runner(x, testname)
            thread_list.append(thread)
        for thread in thread_list:
            thread.start()
            print("Starting Threads")
        for thread in thread_list:
            thread.join()
        time.sleep(1)

def main():
    
    # thread_list = []
    
    # for x in range(1):
    #     thread = runner(x)
    #     thread_list.append(thread)
    # for thread in thread_list:
    #     thread.start()
    #     print("Starting Threads")
    # for thread in thread_list:
    #     thread.join()
    
    runTests(1,1, "MRConsistentReliabilityThreshold")

    # checkMRConsistentReliabilityThreshold(index="smartgrid-2021-05-10t01.53.54.282694", host="192.168.25.19")
    # checkMRConsistentPowerRegulation('smartgridsos')
    # queryESALL('smartgridsos')
    

if __name__ == '__main__':
    main()
