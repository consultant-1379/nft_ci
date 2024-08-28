#!/usr/bin/env python3
import sys
if sys.version_info < (3, 5):
    print("Required Python >= 3.5")
    sys.exit(1)
import argparse
from argparse import Namespace
from subprocess import run, STDOUT, PIPE, CalledProcessError
import json
import yaml
from re import compile as re_compile
from pathlib import Path



def getParameters() -> Namespace:
    """Parse command line parameters
       Add extra parameters needed
    """
    argobj = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter,
                                     description="CDD 2.18 pm-server Work Around")
    argobj.add_argument("--namespace", default="eric-ccsm", help="Kubernetes CCSM namespace")
    argobj.add_argument("--kubeconfig", default="", help="")
    params = argobj.parse_args()

    kubeCfgStr = ""
    if params.kubeconfig not in ["", None]:
        kubeCfgStr = "--kubeconfig {}".format(params.kubeconfig)
    params.kubeCmd = "kubectl {}".format(kubeCfgStr)
    params.kubeCmdNs = "{} -n {}".format(params.kubeCmd, params.namespace)

    return params


def runKubeCmd(command:str, params:Namespace, useNamespace:bool=True) -> tuple:
    """Run Kubectl command in a shell
    """
    status = False
    output = ''
    binary = params.kubeCmd
    if useNamespace:
        binary = params.kubeCmdNs
    try:
        proc = run("{} {}".format(binary, command), shell=True, stdout=PIPE,
                                  stderr=STDOUT, timeout=30, check=False)
        status = proc.returncode == 0
        output = proc.stdout.rstrip()
    except CalledProcessError:
        status = False

    if status is False:
        print("ERROR: Failed to '{}' - output: {}".format(command, output))
        sys.exit(1)

    return output.decode()


def updatePmServerConfig(params:Namespace) -> None:
    """Update eric-pm-server configMap
    """
    cmData = runKubeCmd("get cm eric-ccsm-pm-server-config -o yaml", params)
    regex1 = re_compile("([ \t]+\- 'eric-pm-server.monitoring:[0-9]+')")
    regex2 = re_compile("([ \t]+\- 'eric-pm-server-external.monitoring:[0-9]+')")
    pmServerCfgFile = "pm-server-configmap-new.yaml"
    with open(pmServerCfgFile, "w") as pfile:
        for line in cmData.split('\n'):
            occur = regex2.search(line)
            if occur:
                continue
            occur = regex1.search(line)
            if occur:
                pmserver_orig = occur.group(1)
                pmserver_extra = pmserver_orig.replace('server', 'server-external')
                pfile.write(pmserver_extra + '\n')
            pfile.write(line + '\n')
    runKubeCmd("apply -f ./{}".format(pmServerCfgFile), params, False)
    Path(pmServerCfgFile).unlink()
    runKubeCmd("delete pod eric-pm-server-0", params)


params = getParameters()
updatePmServerConfig(params)
sys.exit(0)
