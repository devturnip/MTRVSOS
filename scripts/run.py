import os
import subprocess
import threading
import re
from datetime import datetime
from pathlib import Path
from random import randrange

from elasticsearch import Elasticsearch
from elasticsearch_dsl import Search

class runner (threading.Thread):
    def __init__(self, name):
        threading.Thread.__init__(self)
        self.name = name
        
    def run(self):
        print("Starting Thread " + self.name)
        executeJarRunner()
        print("Exiting Thread "+ self.name)

def parseException(file):
    with open(file) as f:
        text = f.read()
    exceptions = re.findall(r'(?m)^.*?Exception.*(?:\n+^\s*at .*)+', text, re.M)
    # for exception in exceptions:
    #     print(exception)
        
    return exceptions   

def getJarFile():
    base = Path(__file__).parent.parent / "target"
    for file in os.listdir(base):
        if "shaded" in file:
            return base / file

def executeJarRunner():
    ret, dt_string, ipaddr, fname = executeJar()
    if ret != 0:
        print("failed with return code != 0")
    else:
        checkMRConsistentPowerRegulation(index=dt_string, host=ipaddr, fname=fname)
        
def randomiseVariables():
    # argument_list = []
    
    # #og
    argument_dict = {
        "pg" : randrange(1,10),
        "pd" : randrange(1,10),
        "sh" : randrange(1,10),
        "ev" : randrange(1,10)
    }
    
    #small
    # argument_dict = {
    #     "pg" : randrange(1,3),
    #     "pd" : randrange(1,5),
    #     "sh" : randrange(1,10),
    #     "ev" : randrange(1,10)
    # }
    
    # argument_list.append(pg,pd,sh,ev)
    print(argument_dict)
    return argument_dict

def executeJar():
    var_dict = randomiseVariables()
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
    rt = '120'
    wait = '-wt'
    wt = '6000'
    
    indexName = '-indexname'
    now = datetime.now()
    dt_string = now.strftime("smartgrid-%Y-%m-%dt%H.%M.%S.%f")
    # indexName = {'-indexname' : dt_string}
    
    testrun = '-testrun'
    testname = 'consistentreliabilitythreshold'
    
    elastichost = '-elastichost'
    ipaddr = '192.168.0.31'
    
    jarfile = getJarFile()
    # ret = subprocess.call(['java', '-jar', jarfile, powergen, pg, powerdi, pd, smarthome, sh, electronicveh, ev, runtime, rt, wait, wt, indexName, dt_string, testrun, testname])
    fname = dt_string + "err.txt"
    f = open(fname, "w")
    ret = subprocess.call(['java', '-jar', jarfile, powergen, pg, powerdi, pd, smarthome, sh, electronicveh, ev, runtime, rt, wait, wt, indexName, dt_string, elastichost, ipaddr], stderr=f)
    
    return ret, dt_string, ipaddr, fname


def queryES(index, host):
    host_addr = 'http://' + host + ':9200/'
    client = Elasticsearch([host_addr])
    s = Search(using=client, index=index)
    request = s.source(['hash', 'author_date', 'author'])
        
    response = s.scan()
    return response
        

def queryESALL(index):
    # ElasticSearch instance (url)
    es = Elasticsearch(['http://localhost:9200/'])

    # Build a DSL Search object on the 'commits' index, 'summary' document type
    request = Search(using=es, index=index, doc_type='summary')

    # Run the Search, using the scan interface to get all resuls
    response = request.scan()
    for commit in response:
        print(commit)
        
def checkMRConsistentPowerRegulation(index, host, fname):
    exceptions = parseException(fname)
    if exceptions:
        print("MRConsistentPowerRegulation --", index, "-- result:", "FAILED DUE TO EXCEPTIONS")
        for e in exceptions:
            print(e)
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


def main():
    
    thread_list = []
    
    for x in range(6):
        thread = runner(x)
        thread_list.append(thread)
    for thread in thread_list:
        thread.start()
        print("Starting Threads")
    for thread in thread_list:
        thread.join()

    # checkMRConsistentPowerRegulation('smartgridsos')
    # queryESALL('smartgridsos')
    

if __name__ == '__main__':
    main()
