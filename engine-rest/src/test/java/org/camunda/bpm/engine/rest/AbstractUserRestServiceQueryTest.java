/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.rest;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.Response.Status;

import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.identity.UserQuery;
import org.camunda.bpm.engine.rest.exception.InvalidRequestException;
import org.camunda.bpm.engine.rest.helper.MockProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public abstract class AbstractUserRestServiceQueryTest extends AbstractRestServiceTest {
  
  protected static final String USER_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/user";
  protected static final String USER_COUNT_QUERY_URL = USER_QUERY_URL + "/count";

  private UserQuery mockQuery;

  @Before
  public void setUpRuntimeData() {
    mockQuery = setUpMockUserQuery(MockProvider.createMockUsers());
  }

  private UserQuery setUpMockUserQuery(List<User> list) {
    UserQuery sampleUserQuery = mock(UserQuery.class);
    when(sampleUserQuery.list()).thenReturn(list);
    when(sampleUserQuery.count()).thenReturn((long) list.size());
  
    when(processEngine.getIdentityService().createUserQuery()).thenReturn(sampleUserQuery);
  
    return sampleUserQuery;
  }
  
  @Test
  public void testEmptyQuery() {
    
    String queryKey = "";
    given().queryParam("name", queryKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(USER_QUERY_URL);
    
  }
  
  @Test
  public void testSortByParameterOnly() {
    given().queryParam("sortBy", "firstName")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(USER_QUERY_URL);
  }
  
  @Test
  public void testSortOrderParameterOnly() {
    given().queryParam("sortOrder", "asc")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(USER_QUERY_URL);
  }
  
  @Test
  public void testNoParametersQuery() {
    expect().statusCode(Status.OK.getStatusCode()).when().get(USER_QUERY_URL);
    
    verify(mockQuery).list();
    verifyNoMoreInteractions(mockQuery);
  }
  
  @Test
  public void testSimpleUserQuery() {
    String queryFirstName = MockProvider.EXAMPLE_USER_FIRST_NAME;
    
    Response response = given().queryParam("firstName", queryFirstName)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(USER_QUERY_URL);
    
    InOrder inOrder = inOrder(mockQuery);
    inOrder.verify(mockQuery).userFirstName(queryFirstName);
    inOrder.verify(mockQuery).list();
    
    String content = response.asString();
    List<String> instances = from(content).getList("");
    Assert.assertEquals("There should be one user returned.", 1, instances.size());
    Assert.assertNotNull("The returned user should not be null.", instances.get(0));
    
    String returendLastName = from(content).getString("[0].lastName");
    String returnedFirstName = from(content).getString("[0].firstName");
    String returnedEmail = from(content).getString("[0].email");
    
    Assert.assertEquals(MockProvider.EXAMPLE_USER_FIRST_NAME, returnedFirstName);
    Assert.assertEquals(MockProvider.EXAMPLE_USER_LAST_NAME, returendLastName);
    Assert.assertEquals(MockProvider.EXAMPLE_USER_EMAIL, returnedEmail);
    
  }
  
  @Test
  public void testCompleteGetParameters() {
    
    Map<String, String> queryParameters = getCompleteStringQueryParameters();
    queryParameters.put("memberOfGroup", MockProvider.EXAMPLE_GROUP_ID);
    
    RequestSpecification requestSpecification = given().contentType(POST_JSON_CONTENT_TYPE);
    for (Entry<String, String> paramEntry : queryParameters.entrySet()) {
      requestSpecification.parameter(paramEntry.getKey(), paramEntry.getValue());
    }
    
    requestSpecification.expect().statusCode(Status.OK.getStatusCode())
      .when().get(USER_QUERY_URL);
    
    verify(mockQuery).userEmail(MockProvider.EXAMPLE_USER_EMAIL);
    verify(mockQuery).userFirstName(MockProvider.EXAMPLE_USER_FIRST_NAME);
    verify(mockQuery).userLastName(MockProvider.EXAMPLE_USER_LAST_NAME);
    verify(mockQuery).memberOfGroup(MockProvider.EXAMPLE_GROUP_ID);
    
    verify(mockQuery).list();
    
  }

  private Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<String, String>();
    
    parameters.put("firstName", MockProvider.EXAMPLE_USER_FIRST_NAME);
    parameters.put("lastName", MockProvider.EXAMPLE_USER_LAST_NAME);
    parameters.put("email", MockProvider.EXAMPLE_USER_EMAIL);
  
    return parameters;
  }
  
  @Test
  public void testQueryCount() {
    expect().statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(1))
      .when().get(USER_COUNT_QUERY_URL);
    
    verify(mockQuery).count();
  }
  
  @Test
  public void testSuccessfulPagination() {    
    int firstResult = 0;
    int maxResults = 10;
    given().queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(USER_QUERY_URL);
    
    verify(mockQuery).listPage(firstResult, maxResults);
  }

  
}