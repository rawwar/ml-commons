/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.ml.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.OpenSearchParseException;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.common.ParsingException;
import org.opensearch.core.common.Strings;
import org.opensearch.core.common.bytes.BytesArray;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.ml.common.controller.MLModelController;
import org.opensearch.ml.common.transport.controller.MLUpdateModelControllerAction;
import org.opensearch.ml.common.transport.controller.MLUpdateModelControllerRequest;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.rest.FakeRestRequest;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;

public class RestMLUpdateModelControllerActionTests extends OpenSearchTestCase {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private RestMLUpdateModelControllerAction restMLUpdateModelControllerAction;
    private NodeClient client;
    private ThreadPool threadPool;

    @Mock
    RestChannel channel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        threadPool = new TestThreadPool(this.getClass().getSimpleName() + "ThreadPool");
        client = spy(new NodeClient(Settings.EMPTY, threadPool));
        restMLUpdateModelControllerAction = new RestMLUpdateModelControllerAction();
        doAnswer(invocation -> {
            invocation.getArgument(2);
            return null;
        }).when(client).execute(eq(MLUpdateModelControllerAction.INSTANCE), any(), any());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        threadPool.shutdown();
        client.close();
    }

    @Test
    public void testConstructor() {
        RestMLUpdateModelControllerAction UpdateModelAction = new RestMLUpdateModelControllerAction();
        assertNotNull(UpdateModelAction);
    }

    @Test
    public void testGetName() {
        String actionName = restMLUpdateModelControllerAction.getName();
        assertFalse(Strings.isNullOrEmpty(actionName));
        assertEquals("ml_update_model_controller_action", actionName);
    }

    @Test
    public void testRoutes() {
        List<RestHandler.Route> routes = restMLUpdateModelControllerAction.routes();
        assertNotNull(routes);
        assertFalse(routes.isEmpty());
        RestHandler.Route route = routes.get(0);
        assertEquals(RestRequest.Method.PUT, route.getMethod());
        assertEquals("/_plugins/_ml/model_controllers/{model_id}", route.getPath());
    }

    @Test
    public void testUpdateModelControllerRequest() throws Exception {
        RestRequest request = getRestRequest();
        restMLUpdateModelControllerAction.handleRequest(request, channel, client);
        ArgumentCaptor<MLUpdateModelControllerRequest> argumentCaptor = ArgumentCaptor.forClass(MLUpdateModelControllerRequest.class);
        verify(client, times(1)).execute(eq(MLUpdateModelControllerAction.INSTANCE), argumentCaptor.capture(), any());
        MLModelController updateModelControllerInput = argumentCaptor.getValue().getUpdateModelControllerInput();
        assertEquals("testModelId", updateModelControllerInput.getModelId());
    }

    @Test
    public void testUpdateModelControllerRequestWithEmptyContent() throws Exception {
        exceptionRule.expect(OpenSearchParseException.class);
        exceptionRule.expectMessage("Update model controller request has empty body");
        RestRequest request = getRestRequestWithEmptyContent();
        restMLUpdateModelControllerAction.handleRequest(request, channel, client);
    }

    @Test
    public void testUpdateModelControllerRequestWithNullModelId() throws Exception {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Request should contain model_id");
        RestRequest request = getRestRequestWithNullModelId();
        restMLUpdateModelControllerAction.handleRequest(request, channel, client);
    }

    @Test
    public void testUpdateModelControllerRequestWithNullField() throws Exception {
        exceptionRule.expect(ParsingException.class);
        exceptionRule.expectMessage("expecting token of type [START_OBJECT] but found [VALUE_NULL]");
        RestRequest request = getRestRequestWithNullField();
        restMLUpdateModelControllerAction.handleRequest(request, channel, client);
    }

    private RestRequest getRestRequest() {
        RestRequest.Method method = RestRequest.Method.PUT;
        String requestContent = "{\"user_rate_limiter_config\":{\"testUser\":{}}}";
        Map<String, String> params = Map.of("model_id", "testModelId");
        RestRequest request = new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(method)
            .withPath("/_plugins/_ml/model_controllers/{model_id}")
            .withParams(params)
            .withContent(new BytesArray(requestContent), XContentType.JSON)
            .build();
        return request;
    }

    private RestRequest getRestRequestWithEmptyContent() {
        RestRequest.Method method = RestRequest.Method.PUT;
        Map<String, String> params = Map.of("model_id", "testModelId");
        RestRequest request = new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(method)
            .withPath("/_plugins/_ml/model_controllers/{model_id}")
            .withParams(params)
            .withContent(new BytesArray(""), XContentType.JSON)
            .build();
        return request;
    }

    private RestRequest getRestRequestWithNullModelId() {
        RestRequest.Method method = RestRequest.Method.PUT;
        String requestContent = "{\"user_rate_limiter_config\":{\"testUser\":{}}}";
        Map<String, String> params = new HashMap<>();
        RestRequest request = new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(method)
            .withPath("/_plugins/_ml/model_controllers/{model_id}")
            .withParams(params)
            .withContent(new BytesArray(requestContent), XContentType.JSON)
            .build();
        return request;
    }

    private RestRequest getRestRequestWithNullField() {
        RestRequest.Method method = RestRequest.Method.PUT;
        String requestContent = "{\"user_rate_limiter_config\":{\"testUser\":null}}";
        Map<String, String> params = new HashMap<>();
        params.put("model_id", "testModelId");
        RestRequest request = new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY)
            .withMethod(method)
            .withPath("/_plugins/_ml/model_controllers/{model_id}")
            .withParams(params)
            .withContent(new BytesArray(requestContent), XContentType.JSON)
            .build();
        return request;
    }
}
