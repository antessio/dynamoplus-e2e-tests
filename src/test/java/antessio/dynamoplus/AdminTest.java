package antessio.dynamoplus;

import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorization;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationApiKey;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationHttpSignature;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import antessio.dynamoplus.sdk.domain.system.collection.CollectionBuilder;
import antessio.dynamoplus.sdk.domain.system.index.Index;
import antessio.dynamoplus.sdk.domain.system.index.IndexBuilder;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Unit test for simple App.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminTest {

    public static final String CATEGORY_COLLECTION_NAME = String.format("category_%s", AdminTest.class.getName());
    public static final String BOOK_COLLECTION_NAME = String.format("book_%s", AdminTest.class.getName());

    private SDK sdk = Clients.getIntance().getAdminClient();


    @DisplayName("Test create collections")
    @Test
    @Order(1)
    void testCreateCollections() {

        Collection categoryCollection = getCollection("id", CATEGORY_COLLECTION_NAME);
        Collection bookCollection = getCollection("isbn", BOOK_COLLECTION_NAME);

        Either<Collection, SdkException> collectionResult = sdk.createCollection(categoryCollection);
        assertCollectionMatches(collectionResult, CATEGORY_COLLECTION_NAME, "id");

        Either<Collection, SdkException> bookResult = sdk.createCollection(bookCollection);
        assertCollectionMatches(bookResult, BOOK_COLLECTION_NAME, "isbn");

    }


    @DisplayName("Test create indexes")
    @Test
    @Order(2)
    void testCreateIndexes() {
        testIndex(CATEGORY_COLLECTION_NAME, "category__name", Collections.singletonList("name"), getCollection("id", CATEGORY_COLLECTION_NAME));
        testIndex(BOOK_COLLECTION_NAME, "book__author", Collections.singletonList("author"), getCollection("id", BOOK_COLLECTION_NAME));
        testIndex(BOOK_COLLECTION_NAME, "book__title", Collections.singletonList("title"), getCollection("id", BOOK_COLLECTION_NAME));
        testIndex(BOOK_COLLECTION_NAME, "book__category.name", Collections.singletonList("category.name"), getCollection("id", BOOK_COLLECTION_NAME));
    }


    @DisplayName("Create client authorization API key read only")
    @Test
    @Order(3)
    void createClientAuthorizationApiKeyReadOnly() {
        List<ClientScope> scopes = ClientScope.READ.stream().map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType)).collect(Collectors.toList());
        String clientIdApiKeyReadOnly = Clients.getIntance().getClientIdApiKeyReadOnly();
        String keyId = Clients.getIntance().getKeyId();
        ClientAuthorizationApiKey clientAuthorization = new ClientAuthorizationApiKey(clientIdApiKeyReadOnly, scopes, keyId, Collections.emptyList());
        testClientAuthorizationApiKey(clientAuthorization);
    }

    @DisplayName("Create client authorization API key")
    @Test
    @Order(4)
    void createClientAuthorizationApiKey() {
        List<ClientScope> scopes = Stream.concat(
                ClientScope.READ_WRITE.stream().map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType)),
                ClientScope.READ.stream().map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType))
        ).collect(Collectors.toList());
        String clientIdApiKey = Clients.getIntance().getClientIdApiKey();
        String keyId = Clients.getIntance().getKeyId();
        ClientAuthorizationApiKey clientAuthorization = new ClientAuthorizationApiKey(clientIdApiKey, scopes, keyId, Collections.emptyList());
        testClientAuthorizationApiKey(clientAuthorization);
    }


    @DisplayName("Create client authorization http signature read only")
    @Test
    @Order(5)
    void createClientAuthorizationHttpSignatureReadOnly() {
        List<ClientScope> scopes = ClientScope.READ.stream().map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType)).collect(Collectors.toList());
        String clientIdHttpSignatureReadOnly = Clients.getIntance().getClientIdHttpSignatureReadOnly();
        String publicKey = Clients.getIntance().getPublicKey();
        ClientAuthorizationHttpSignature clientAuthorization = new ClientAuthorizationHttpSignature(clientIdHttpSignatureReadOnly, scopes, publicKey);
        testClientAuthorizationHttpSignature(clientAuthorization);
    }

    @DisplayName("Create client authorization http signature")
    @Test
    @Order(6)
    void createClientAuthorizationHttpSignature() {
        List<ClientScope> scopes = Stream.concat(
                ClientScope.READ_WRITE.stream().map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType)),
                ClientScope.READ.stream().map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType))
        ).collect(Collectors.toList());
        String clientIdHttpSignature = Clients.getIntance().getClientIdHttpSignature();
        String publicKey = Clients.getIntance().getPublicKey();
        ClientAuthorizationHttpSignature clientAuthorization = new ClientAuthorizationHttpSignature(clientIdHttpSignature, scopes, publicKey);
        testClientAuthorizationHttpSignature(clientAuthorization);
    }

    @DisplayName("Get client api key")
    @Test
    @Order(7)
    void getClientApiKey() {
        String clientIdApiKey = Clients.getIntance().getClientIdApiKey();
        Either<ClientAuthorizationApiKey, SdkException> result = sdk.getClientAuthorizationApiKey(clientIdApiKey);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(c -> c.getType().equals(ClientAuthorization.ClientAuthorizationType.api_key));
    }


    private void testClientAuthorizationApiKey(ClientAuthorizationApiKey clientAuthorization) {
        Either<ClientAuthorizationApiKey, SdkException> result = sdk.createClientAuthorizationApiKey(clientAuthorization);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(c -> c.getClientId().equals(clientAuthorization.getClientId()));
    }

    private void testClientAuthorizationHttpSignature(ClientAuthorizationHttpSignature clientAuthorization) {
        Either<ClientAuthorizationHttpSignature, SdkException> result = sdk.createClientAuthorizationHttpSignature(clientAuthorization);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(c -> c.getClientId().equals(clientAuthorization.getClientId()));
    }


    private void testIndex(String category, String name, List<String> conditions, Collection collection) {
        Either<Index, SdkException> resultCreateIndex1 = sdk.createIndex(new IndexBuilder()
                .uid(UUID.randomUUID())
                .collection(collection)
                .name(name)
                .orderingKey(null)
                .conditions(conditions)
                .createIndex()
        );
        assertIndexMatches(resultCreateIndex1, category, name);
    }


    private Collection getCollection(String idKey, String collectionName) {
        return new CollectionBuilder()
                .idKey(idKey)
                .name(collectionName)
                .fields(Collections.emptyList())
                .createCollection();
    }

    private void assertIndexMatches(Either<Index, SdkException> indexResult, String collectionName, String indexName) {
        indexResult.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(indexResult.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(i -> i.getUid() != null, "uid must be present"))
                .hasValueSatisfying(new Condition<>(i -> i.getCollection().getName().equals(collectionName), "collection name must match"))
                .hasValueSatisfying(new Condition<>(i -> i.getName().equals(indexName), "index name must match"));
    }

    private void assertCollectionMatches(Either<Collection, SdkException> collectionResult, String book, String isbn) {
        collectionResult.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(collectionResult.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(c -> c.getName().equals(book), "collection name must match"))
                .hasValueSatisfying(new Condition<>(c -> c.getIdKey().equals(isbn), "id name must match"));
    }
}
