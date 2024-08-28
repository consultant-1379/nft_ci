#!/usr/bin/env python3
#

'''
    Verify feature files
'''

import os
import sys
import atexit
import shutil
import logging
import argparse
import tempfile

from behave.__main__ import run_behave
from behave.configuration import Configuration


WORKSPACE = None


@atexit.register
def clean_workspace(*args):
    '''Remove workspace'''
    if WORKSPACE is not None:
        try:
            shutil.rmtree(WORKSPACE, ignore_errors=True)
        except Exception as error:
            logging.error(f'Cannot remove temporal workspace "{WORKSPACE}": {error}')


def main():
    '''Main program'''
    global WORKSPACE
    user_options = parse_commandline()
    if not user_options:
        return -1

    WORKSPACE = tempfile.mkdtemp()

    logging.info('Populating workspace...')
    for feature in user_options.feature:
        logging.debug(f'Adding {feature}')        
        shutil.copy(feature, WORKSPACE)

    logging.info('Adding steps file...')
    steps_dir = os.path.join(WORKSPACE, 'steps')
    os.makedirs(steps_dir, exist_ok=True)
    with open(os.path.join(steps_dir, '__init__.py'), 'w') as contents:
        contents.write('''# Test steps verifier
''')
    with open(os.path.join(steps_dir, 'steps.py'), 'w') as contents:
        contents.write('''# Test steps verifier
import logging
DEB = logging.debug


## GIVEN ##
@given(u'target type is {target}')
def set_target(context, target): # pragma: no cover
    DEB(f'** set_target({context}, {target})')

@given(u'target tag is {tag}')
def set_tag(context, tag): # pragma: no cover
    DEB(f'** set_tag({context}, {tag})')

@given(u'path is {path}')
def set_path(context, path): # pragma: no cover
    DEB(f'** set_path({context}, {path})')

@given(u'Content-Length is calculated automatically')
def compute_content_length(context): # pragma: no cover
    DEB('f** compute_content_length({context})')

@given(u'request header is {header}')
def add_header(context, header): # pragma: no cover
    DEB(f'** add_header({context}, {header})')

@given(u'request content is')
def set_content(context): # pragma: no cover
    DEB(f'** set_content({context}, {context.text})')

@given(u'incoming HTTP request to generic server number {server_id}')
def set_incoming_request_server(context, server_id): # pragma: no cover
    DEB(f'** set_incoming_request_server({context}, {server_id})')

@given(u'incoming HTTP request path prefix {prefix}')
def set_incoming_request_prefix(context, prefix): # pragma: no cover
    DEB(f'** set_incoming_request_prefix({context}, {prefix})')

@given(u'callback request content before key {key}')
def set_callback_content(context, key):
    DEB(f'** set_callback_content({context}, {key})')

@given(u'callback request to server number {server_id} type {server_type}')
def set_callback_server_number(context, server_id, server_type): # pragma: no cover
    DEB(f'** set_callback_server_numer({context}, {server_id}, {server_type})')

@given(u'callback request to server type {server_type}')
def set_callback_server_type(context, server_type):  # pragma: no cover
    DEB(f'** set_callback_server_type({context}, {server_type})')

@given(u'callback request path prefix {path_prefix}')
def set_callback_path(context, path_prefix): # pragma: no cover
    DEB(f'** set_callback_path({context}, {path_prefix})')

@given(u'callback request with before key {before_key}')
def set_callback_before_key(context, before_key): # pragma: no cover
    DEB(f'** set_callback_before_key({context}, {before_key})')

@given(u'callback request with after key {after_key}')
def set_callback_after_key(context, after_key): # pragma: no cover
    DEB(f'** set_callback_after_key({context}, {after_key})')

@given(u'callback handler action name is {callback_action_name}')
def set_callback_handler_name(context, callback_action_name): # pragma: no cover
    DEB(f'** set_callback_action_name({context}, {callback_action_name})')

@given(u'action name is {action}')
def set_action_name(context, action): # pragma: no cover
    DEB(f'** set_action_name({context}, {action})')

@given(u'{parameter} computed using {algorithm}')
def compute_parameter(context, parameter, algorithm): # pragma: no cover
    DEB(f'** compute_parameter({context}, {parameter}, {algorithm})')

# GIVEN: Diameter
@given(u'Diameter target type is {target}')
def dia_target_type(context, target):
    DEB(u'** dia_target_type({context}, {target})')

@given(u'Diameter application is {target}')
def dia_app_is(context, target):
    DEB(u'** dia_app_is({context}, {target})')

@given(u'Diameter request name is Authentication-Information-Request')
def dia_req_name1(context):
    DEB(u'** dia_req_name1({context})')

@given(u'Diameter request name is Update-Location-Request')
def dia_req_name2(context):
    DEB(u'** dia_req_name2({context})')

@given(u'Diameter request name is Purge-UE-Request')
def dia_req_name3(context):
    DEB(u'** dia_req_name3({context})')

@given(u'Diameter request name is Notify-Request')
def dia_req_name4(context):
    DEB(u'** dia_req_name4({context})')

@given(u'Diameter request is proxyable')
def dia_req_is(context):
    DEB(u'** dia_req_is({context})')

@given(u'Diameter request AVPs are')
def dia_req_are(context):
    DEB(u'** dia_req_are({context})')

@given(u'Diameter request name is Location-Info-Request')
def dia_req_loc(context):
    DEB(u'** dia_req_loc({context})')

@given(u'Diameter request name is LCS-Routing-Info-Request')
def dia_req_lcs_rou(context):
    DEB(u'** dia_req_lcs_rou({context})')

@given(u'Diameter request name is Multimedia-Auth-Request')
def dia_req_mul(context):
    DEB(u'** dia_req_mul({context})')

@given(u'Diameter request name is Profile-Update-Request')
def dia_req_pro(context):
    DEB(u'** dia_req_pro({context})')

@given(u'Diameter request name is Server-Assignment-Request')
def dia_req_ser(context):
    DEB(u'** dia_req_ser({context})')

@given(u'Diameter request name is Subscribe-Notifications-Request')
def dia_req_sub(context):
    DEB(u'** dia_req_sub({context})')

@given(u'Diameter request name is User-Authorization-Request')
def dia_req_name1(context):
    DEB(f'** dia_req_name1({context})')

@given(u'Diameter request name is User-Data-Request')
def dia_req_name2(context):
    DEB(f'** dia_req_name2({context})')

@given(u'Incoming Diameter Insert-Subscriber-Data-Request target type is EPC')
def inc_dia_ins1(context):
    DEB(u'** inc_dia_ins1({context})')

@given(u'Incoming Diameter Insert-Subscriber-Data-Request application is S6a')
def inc_dia_ins1(context):
    DEB(u'** inc_dia_ins1({context})')
    
@given(u'Incoming Diameter Insert-Subscriber-Data-Request key AVP is User-Name')
def inc_dia_ins2(context):
    DEB(u'** inc_dia_ins2({context})')

@given(u'Incoming Diameter Cancel-Location-Request target type is EPC')
def inc_dia_cancel1(context):
    DEB(u'** inc_dia_cancel1({context})')

@given(u'Incoming Diameter Cancel-Location-Request application is S6a')
def inc_dia_cancel2(context):
    DEB(u'** inc_dia_cancel2({context})')

@given(u'Incoming Diameter Cancel-Location-Request key AVP is User-Name')
def inc_dia_cancel3(context):
    DEB(u'** inc_dia_cancel3({context})')

@given(u'SOAP target type is PG_SOAP')
def soap_tar_type(context):
    DEB(u'** soap_tar_type({context})')

@given(u'SOAP action name is UpdateEPCProfile')
def soap_act_namsvc(context):
    DEB(u'** soap_act_namsvc({context})')

@given(u'SOAP request service is /HssEsmUdcWS/services/NotificationService')
def soap_req_svc(context):
    DEB(u'** soap_req_svc({context})')

@given(u'SOAP request action is /HssEsmUdcWS/services/NotificationService')
def soap_req_act(context):
    DEB(u'** soap_req_act({context})')

@given(u'SOAP request property ${Properties#mscId} equals {mscidprefix}{mscid}')
def soap_req_prop1(context,mscidprefix,mscid):# pragma: no cover
    DEB(f'** soap_req_prop1({context}, {mscidprefix}, {mscid})')

@given(u'SOAP request property ${Properties#imsi} equals {imsi}')
def soap_req_prop2(context,imsi):# pragma: no cover
    DEB(f'** soap_req_prop2({context}, {imsi})')

@given(u'SOAP request property ${Properties#mme} equals {EPC_CLIENT_ORIGIN_HOST}')
def soap_req_prop3(context,EPC_CLIENT_ORIGIN_HOST):# pragma: no cover
    DEB(f'** soap_req_prop3({context}, {EPC_CLIENT_ORIGIN_HOST})')

@given(u'SOAP request property ${Properties#realm} equals {EPC_CLIENT_ORIGIN_REALM}')
def soap_req_prop4(context,EPC_CLIENT_ORIGIN_REALM):# pragma: no cover
    DEB(f'** soap_req_prop4({context}, {EPC_CLIENT_ORIGIN_REALM})')

@given(u'SOAP request property ${Properties#aaa} equals {EPC_CLIENT_ORIGIN_HOST}')
def soap_req_prop5(context,EPC_CLIENT_ORIGIN_HOST):# pragma: no cover
    DEB(f'** soap_req_prop5({context}, {EPC_CLIENT_ORIGIN_HOST})')

@given(u'SOAP request envelope is')
def soap_req_env(context):
    DEB(u'** soap_req_dnv({context})')

@given(u'Diameter request name is ME-Identity-Check-Request')
def dia_req_me(context):
    DEB(u'** dia_req_me({context})')

@given(u'callback request content after key "')
def icallback(context):
    DEB(u'** callback({context})')

@given(u'Incoming Diameter Registration-Termination-Request target type is IMS')
def inc_dia_type(context):
    DEB(u'** inc_dia_type({context})')

@given(u'Incoming Diameter Registration-Termination-Request target type is EPC')
def inc_dia_type(context):
    DEB(u'** inc_dia_type({context})')

@given(u'Incoming Diameter Registration-Termination-Request application is Cx')
def inc_dia_app(context):
    DEB(u'** inc_dia_app({context})')

@given(u'Incoming Diameter Registration-Termination-Request application is Swx')
def inc_dia_app(context):
    DEB(u'** inc_dia_app({context})')

@given(u'Incoming Diameter Registration-Termination-Request key AVP is User-Name')
def inc_dia_tkey(context):
    DEB(u'** inc_dia_key({context})')

@given(u'Incoming Diameter Push-Profile-Request target type is IMS')
def inc_dia_push_type(context):
    DEB(u'** inc_dia_push_type({context})')

@given(u'Incoming Diameter Push-Profile-Request application is Cx')
def inc_dia_push_app(context):
    DEB(u'** inc_dia_push_app({context})')
@given(u'Incoming Diameter Push-Profile-Request key AVP is User-Name')
def inc_dia_push_user(context):
    DEB(u'** inc_dia_push_user({context})')

@given(u'Incoming Diameter Delete-Subscriber-Data-Request target type is EPC')
def inc_dia_push_del1(context):
    DEB(u'** inc_dia_push_del1({context})')
@given(u'Incoming Diameter Delete-Subscriber-Data-Request application is S6a')
def inc_dia_push_del2(context):
    DEB(u'** inc_dia_push_del2({context})')
@given(u'Incoming Diameter Delete-Subscriber-Data-Request key AVP is User-Name')
def inc_dia_push_del3(context):
    DEB(u'** inc_dia_push_del3({context})')
@given(u'SOAP request property ${Properties#oldattr} equals <{keyIndex}:ATTR>')
def soap_req1(context,keyIndex):# pragma: no cover
    DEB(u'** soap_req1({context}{keyIndex})')
@given(u'SOAP request property ${Properties#oldvalue} equals <{keyIndex}:VAL>')
def soap_req2(context,keyIndex):# pragma: no cover
    DEB(u'** soap_req2({context}{keyIndex})')

@given(u'SOAP request property ${Properties#old_attr} equals {old_attr_prefix}{mscid}')
def soap_req3(context,old_attr_prefix,mscid):# pragma: no cover
    DEB(u'** soap_req3{context}{old_attr_prefix}{mscid})')

@given(u'SOAP request property ${Properties#new_attr} equals {new_attr_prefix}{mscid}')
def soap_req4(context,new_attr_prefix,mscid):# pragma: no cover
    DEB(u'** soap_req4({context}{new_attr_prefix}{mscid})')

@given(u'SOAP request property ${Properties#oldvalue} equals <{keyIndex}:OLD_VAL>')
def soap_req5(context,keyIndex):# pragma: no cover
    DEB(u'** soap_req5({context}{keyIndex})')

@given(u'SOAP request property ${Properties#newvalue} equals <{keyIndex}:NEW_VAL>')
def soap_req6(context,keyIndex):# pragma: no cover
    DEB(u'** soap_req6({context}{keyIndex})')

@given(u'callback request HTTP method POST')
def callback_post(context):
    DEB(u'** callback_post({context})')

@given(u'callback request HTTP method PUT')
def callback_put(context):
    DEB(u'** callback_put({context})')

@given(u'callback request HTTP method DELETE')
def callback_del(context):
    DEB(u'** callback_del({context})')

@given(u'Incoming Diameter Push-Notification-Request target type is IMS')
def inc_dia_push1(context):
    DEB(u'** inc_dia_push1({context})')

@given(u'Incoming Diameter Push-Notification-Request application is Sh')
def inc_dia_push2(context):
    DEB(u'** inc_dia_push2({context})')

@given(u'Incoming Diameter Push-Notification-Request key AVP is Public-Identity')
def inc_dia_push3(context):
    DEB(u'** inc_dia_push3({context})')

@given(u'callback request content property named subscription.{key}')
def callback_req_property(context, key):
    DEB(u'** callback_req_property({context},{key})')

# THEN : Diameter
@then(u'we expect Diameter response time less than {interval} milliseconds')
def dia_expect_time(context,interval):
    DEB(u'** dia_expect_time({context}, {interval})')

@then(u'save Incoming Diameter Insert-Subscriber-Data-Request AVP Session-Id value as sessionId')
def save_in_dia(context):
    DEB(u'** save_in_dia({context})')

@then(u'we expect Diameter result DIAMETER_ERROR_TRANSPARENT_DATA_OUT_OF_SYNC')
def dia_result(context):
    DEB(u'** dia_result({context})')

@then(u'we expect Diameter result DIAMETER_SUBSEQUENT_REGISTRATION')
def dia_result(context):
    DEB(u'** dia_result({context})')

@then(u'we expect Diameter result DIAMETER_SUCCESS')
def dia_expect_res1(context):
    DEB(u'** dia_expect_res1({context})')

@then(u'we expect Diameter result DIAMETER_FIRST_REGISTRATION')
def dia_expect_res2(context):
    DEB(u'** dia_expect_res2({context})')

@then(u'we expect Diameter answer contains AVPs')
def dia_expect_answ(context):
    DEB(u'** dia_expect_answ({context})')

@then(u'we expect Diameter result DIAMETER_UNREGISTERED_SERVICE')
def dia_expect_res3(context):
    DEB(u'** dia_expect_res3({context})')

@then(u'we expect Diameter result DIAMETER_ERROR_IN_ASSIGNMENT_TYPE')
def dia_error_assign(context):
    DEB(u'** dia_error_assign({context})')

@then(u'we send Diameter Push-Notification-Answer with target type IMS, application Sh and AVPs')
def dia_push_notif_answer(context):
    DEB(u'** dia_push_notif_answer({context})')

@then(u'we wait for Diameter Cancel-Location-Request for {interval} seconds')
def dia_cancel_loc_req(context, interval):
    DEB(u'** dia_cancel_loc_req({context}, {interval})')

@then(u'we wait for Diameter Push-Profile-Request for {interval} seconds')
def dia_push_prof_req(context, interval):
    DEB(u'** dia_push_prof_req({context},{interval})')

@then(u'we wait for Diameter Insert-Subscriber-Data-Request for {interval} seconds')
def dia_ins_subs_data_req(context, interval):
    DEB(u'** dia_ins_subs_data_req({context},{interval})')

@then(u'we store key {token} value {fetchedToken}')
def store_key_token(context,token,fetchedToken):
    DEB(u'** store_key_token{context},{token},{fetchedToken})')

# THEN : Diameter
@then(u'we send Diameter answer for Insert-Subscriber-Data-Request with AVPs')
def send_dia(context):
    DEB(u'** send_dia({context})')

@then(u'we expect SOAP response has no fault')
def exp_soap_resp(context):
    DEB(u'** exp_soap_resp({context})')

@then(u'we expect SOAP response time less than {interval} milliseconds')
def exp_soap_resp_time(context, interval):
    DEB(u'** exp_soap_resp_time({context}, {interval})')

@then(u'save Incoming Diameter Cancel-Location-Request AVP Session-Id value as sessionId')
def dia_cancel_loc1(context):
    DEB(u'** dia_cancel_loc1({context})')

@then(u'we send Diameter answer for Cancel-Location-Request with AVPs')
def dia_cancel_loc2(context):
    DEB(u'** dia_cancel_loc2({context})')

@then(u'we send Diameter answer for Registration-Termination-Request with AVPs')
def dia_reg_term1(context):
    DEB(u'** dia_reg_term1({context})')

@then(u'save Incoming Diameter Registration-Termination-Request AVP Session-Id value as sessionId')
def dia_reg_term3(context):
    DEB(u'** dia_reg_term3({context})')

@then(u'save Incoming Diameter Push-Profile-Request AVP Session-Id value as sessionId')
def dia_reg_term4(context):
    DEB(u'** dia_reg_term5({context})')

@then(u'we send Diameter answer for Push-Profile-Request with AVPs')
def dia_reg_term5(context):
    DEB(u'** dia_reg_term5({context})')

@then(u'save response content property access_token value as fetchedToken')
def save_resp1(context):
    DEB(u'** save_resp1({context})')

@then(u'we wait for Diameter Registration-Termination-Request for {interval} seconds')
def wait_dia_reg(context, interval):
    DEB(f'** wait_dia_reg({context}, {interval})')

@then(u'we send Diameter answer for Push-Notification-Request with AVPs')
def wait_dia_push2(context):
    DEB(f'** wait_dia_push2({context})')

@then(u'we wait for Diameter Push-Notification-Request for {interval} seconds')
def wait_dia_push3(context, interval):
    DEB(f'** wait_dia_push3({context}, {interval})')

@then(u'save Incoming Diameter Push-Notification-Request AVP Session-Id value as sessionId')
def wait_dia_push4(context):
    DEB(f'** wait_dia_push4({context})')

@then(u'we expect Diameter response before action CLR_Verify')
def expect_dia_clr_verify(context):
    DEB(f'** expect_dia_clr_verify({context})')

@then(u'we expect Diameter response before action IDR_Verify')
def expect_dia_idr_verify(context):
    DEB(f'** expect_dia_idr_verify({context})')

@then(u'we save incoming callback request content property named subscription.{subs}')
def save_in_call(context, subs):
    DEB(f'** save_in_call({context},{subs})')

## WHEN ##

@when(u'we receive incoming Diameter Insert-Subscriber-Data-Request for {imsi}')
def rcv_in_rq(context, imsi): # pragma: no cover
    DEB(f'** rcv_in_rq({context}, {imsi})')

@when(u'we send {operation} request')
def send_request(context, operation): # pragma: no cover
    DEB(f'** send_request({context}, {operation})')

@when(u'we receive incoming HTTP requests')
def receive_request(context): # pragma: no cover
    DEB(f'** receive_request({context})')

@when(u'we receive callback request')
def wait_callback_request(context): # pragma: no cover
    DEB(f'** wait_callback_request({context})')

@when(u'we receive incoming Diameter Cancel-Location-Request for {imsi}')
def inc_dia_cancel_loc(context,imsi):
    DEB(u'** inc_dia_cancel_loc({context}, {imsi})')

@when(u'we receive Diameter Push-Notification-Request')
def dia_push_notif_request(context):
    DEB(u'** dia_push_notif_request({context})')

@when(u'we receive incoming Diameter Registration-Termination-Request for UserName{mscid}_0@ericsson.se')
def dia_reg_term1(context,mscid):
    DEB(u'** dia_reg_term1({context},{mscid})')

@when(u'we receive incoming Diameter Registration-Termination-Request for {imsi}@ims.mnc280.mcc262.3gppnetwork.org')
def dia_push_profile_reg(context,imsi):
    DEB(u'** dia_push_profile_reg({context},{imsi})')

@when(u'we receive incoming Diameter Registration-Termination-Request for {imsi}')
def dia_push_profile_reg(context,imsi):
    DEB(u'** dia_push_profile_reg({context},{imsi})')

@when(u'we receive incoming Diameter Push-Profile-Request for {imsi}@ims.mnc280.mcc262.3gppnetwork.org')
def dia_push_profile2(context, imsi):
    DEB(u'** dia_push_profile2({context}, {imsi})')

@when(u'we receive incoming Diameter Delete-Subscriber-Data-Request for {imsi}')
def inc_dia_del(context,imsi):
    DEB(u'** inc_dia_del({context}, {imsi})')

@when(u'we receive Diameter Insert-Subscriber-Data-Request')
def rec_dia1(context):
    DEB(u'** rec_dia1({context})')

@when(u'we receive Diameter Cancel-Location-Request')
def rec_dia2(context):
    DEB(u'** rec_dia2({context})')
   
@when(u'we receive incoming Diameter Push-Notification-Request for sip:UserName<mscid>_0PublicID0@ericsson.se')
def rec_dia_push(context):
    DEB(u'** rec_dia_push({context})')

@when(u'we receive default incoming Diameter Push-Notification-Request no key')
def rec_dia_push2(context):
    DEB(u'** rec_dia_push2({context})')

## THEN ##
@then(u'Sleep for {amount} seconds')
def sleep(context, amount): # pragma: no cover
    DEB(f'** sleep({context}, {amount})')

@then(u'we expect response status code {status}')
def check_status_code(context, status): # pragma: no cover
    DEB(f'** check_status_code({context}, {status})')

@then(u'compute {parameter} using {algorithm} algorithm')
def check_response(context, parameter, algorithm): # pragma: no cover
    DEB(f'** check_response({context}, {parameter}, {algorithm})')

@then(u'extract path from URI in response content property {property_name} and save as {name}')
def extract_path_from_URI(context, property_name, name): # pragma: no cover
    DEB(f'** extract_path_from_URI({context}, {property_name}, {name})')

@then(u'we expect callback request')
def check_callback_request(context): # pragma: no cover
    DEB(f'** check_callback_request({context})')

@then(u'we expect response content text property {property_name} equals {value}')
def check_content_property(context, property_name, value): # pragma: no cover
    DEB(f'** check_content_property({context}, {property_name}, {value})')

@then(u'we send response with status code {status}')
def send_response(context, status): # pragma: no cover
    DEB(f'** send_response({context}, {status})')

@then(u'we wait for callback request')
def check_wait_callback(context): # pragma: no cover
    DEB(f'** check_wait_callback({context})')

@then(u'we expect response time less than {interval} milliseconds')
def check_response_time(context, interval): # pragma: no cover
    DEB(f'** check_response_time({context}, {interval})')

@then(u'we send callback response with status code {status}')
def send_callback_response(context, status): # pragma: no cover
    DEB(f'** send_callback_response({context}, {status})')

@then(u'we send default response with status code {status}')
def send_default_response(context, status): # pragma: no cover
    DEB(f'** send_default_response({context}, {status})')

@then(u'we send HTTP response with content')
def send_http_response_with_content(context): # pragma: no cover
    DEB(f'** send_http_response_with_content({context}, {context.text})')

@then(u'we send HTTP response with status code {status}')
def send_http_response(context, status): # pragma: no cover
    DEB(f'** send_http_response({context}, {status})')

@then(u'we send default response with header content-type:application/json and status code 204')
def step_impl(context): # pragma: no cover
    DEB(f'** step_impl_status({context})')
    
@then(u'we send default response with header content-type:application/json and status code 204 {status}')
def step_impl_status(context, status): # pragma: no cover
    DEB(f'** step_impl_status({context}, {status})')

@then(u'we send HTTP response with {header} header equal to {value}')
def send_http_response_with_header(context, header, value): # pragma: no cover
    DEB(f'** send_http_response_with_header({context}, {header}, {value})')
    
@then(u'save Incoming Diameter Delete-Subscriber-Data-Request AVP Session-Id value as sessionId')
def save_in_del(context):
    DEB(f'** save_in_del({context})')

@then(u'we send Diameter answer for Delete-Subscriber-Data-Request with AVPs')
def send_dia_ans(context):
    DEB(f'** send_dia_ans({context})')

@then(u'we wait for Diameter Delete-Subscriber-Data-Request for {interval} seconds')
def wait_dia(context, interval):
    DEB(f'** step_impl({context},{interval})')

@then(u'save Diameter Insert-Subscriber-Data-Request AVP Session-Id value as sessionId')
def save_dia1(context):
    DEB(f'** save_dia1({context})')

@then(u'we send Diameter Insert-Subscriber-Data-Answer with target type EPC, application S6a and AVPs')
def send_dia1(context):
    DEB(f'** send_dia1({context})')

@then(u'save Diameter Cancel-Location-Request AVP Session-Id value as sessionId')
def save_dia2(context):
    DEB(f'** save_dia2({context})')

@then(u'we send Diameter Cancel-Location-Answer with target type EPC, application S6a and AVPs')
def send_dia2(context):
    DEB(f'** send_dia2({context})')


''')

    logging.info(f'Compiling feature: {feature}')

    #behave_config = Configuration([WORKSPACE, '-f', 'null', '--no-capture'])
    behave_config = Configuration([WORKSPACE])
    errors = run_behave(behave_config)
    print(f'Errors: {errors}')

    return errors


def parse_commandline():
    '''Parse and check command line'''
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__), description=__doc__)

    parser.add_argument('feature', nargs='+', help='Feature files to check')

    parser.add_argument('-f', '--force', action='store_true', default=False, help='Ignore compilation errors and continue', dest='ignore_errors')
    parser.add_argument('-d', '--debug', action='store_true', default=False, help='Show debug logging', dest='debug')

    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG if args.debug else logging.INFO)

    for feature_file in args.feature:
        if not os.path.exists(feature_file):
            logging.error(f'Feature file "{feature_file}" not exists!')
            return None

    return args


if __name__ == '__main__':
    sys.exit(main())

