/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.client.models.PostClientsRequest;
import org.apache.fineract.client.models.PostClientsResponse;
import org.apache.fineract.client.models.PostSavingsAccountsRequest;
import org.apache.fineract.client.models.PostSavingsAccountsResponse;
import org.apache.fineract.integrationtests.client.IntegrationTest;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

public class SavingsAccountsClientBirthdayTest extends IntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SavingsAccountsClientBirthdayTest.class);
    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;
    private SavingsAccountHelper savingsAccountHelper;
    private SavingsProductHelper savingsProductHelper;
    private ClientHelper clientHelper;
    private final String dateFormat = "yyyy-MM-dd";
    public static final String DEPOSIT_AMOUNT = "2000";
    public static final String WITHDRAW_AMOUNT = "1000";
    public static final String WITHDRAW_AMOUNT_ADJUSTED = "500";
    public static final String MINIMUM_OPENING_BALANCE = "1000.0";
    public static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";


    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.savingsProductHelper = new SavingsProductHelper();
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
    }

    //this is a very basic test and we would want to implement a more thorough retrieval test.
    @Test
    void testBirthdayRetrieval() {
        LOG.info("test retrieving accounts given birthday:");

        LocalDate birthDate = LocalDate.of(1997, 1, 29);
        PostClientsRequest clientRequest = ClientHelper.defaultClientCreationRequest();
        clientRequest.setActivationDate(LocalDate.now().toString());
        clientRequest.dateOfBirth(birthDate);
        clientRequest.setDateFormat(dateFormat);
        PostClientsResponse clientResponse = ok(fineractClient().clients.create6(clientRequest));
        assertNotNull(clientResponse);
        Long clientId = clientResponse.getClientId();



        final String savingsProductJSON = savingsProductHelper.build();
        final Integer savingsProductID = SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);        
        assertNotNull(savingsProductID);

        PostSavingsAccountsRequest savingsAccRequest = new PostSavingsAccountsRequest();
        savingsAccRequest.setClientId(clientId);
        savingsAccRequest.setProductId(savingsProductID.longValue());
        savingsAccRequest.setLocale("en");
        savingsAccRequest.submittedOnDate(LocalDate.now().toString());
        savingsAccRequest.setDateFormat(dateFormat);
        PostSavingsAccountsResponse savingsAccResponse = ok(fineractClient().savingsAccounts.submitApplication2(savingsAccRequest));
        assertNotNull(savingsAccResponse);
        Long savingsAccountId = savingsAccResponse.getSavingsId();


        //this method is deprecated, ultimately should rewrite this when adding more thorough test cases.
        String testUrl = "/fineract-provider/api/v1/savingsaccounts/birthday?dateOfBirth=" + LocalDate.of(1997, 1, 29).toString() + "&" + Utils.TENANT_IDENTIFIER;
        String response = Utils.performServerGet(requestSpec, responseSpec, testUrl);
        LOG.info("get response:\n ", response);
        List<Integer> birthdayIds =  JsonPath.from(response).getList("pageItems.id");

        boolean accountFound = false;
        for (Integer id : birthdayIds) {
        if (id.longValue() == savingsAccountId) {
            accountFound = true;
            break;
            }
        }
        assertTrue(accountFound);
    }

    @Test
    void testEmptyBirthdayRetrieval() {
        LOG.info("test nonexistent birthday: ");
        String testUrl = "/fineract-provider/api/v1/savingsaccounts/birthday?dateOfBirth=" + LocalDate.of(1412, 1, 1).toString() + "&" + Utils.TENANT_IDENTIFIER;

        String responseBody = Utils.performServerGet(requestSpec, responseSpec, testUrl);
        JsonPath jsonPath = new JsonPath(responseBody);
        List<Object> items = jsonPath.getList("pageItems");
        assertThat(items).isEmpty();
    }

    //ideally we never have 500 errors, This is something to be investigated and changed in the future.
    @Test
    void testInvalidBirthdayFormat() {
        LOG.info("testing various invalid uri queries:");
        String testUrl = "/fineract-provider/api/v1/savingsaccounts/birthday?dateOfBirth=failToParse&" + Utils.TENANT_IDENTIFIER;

        ResponseSpecification responseSpec500 = new ResponseSpecBuilder().expectStatusCode(500).build();
        Utils.performServerGet(requestSpec, responseSpec500, testUrl);
    }
}