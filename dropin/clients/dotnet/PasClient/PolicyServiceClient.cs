namespace Acme.Pas.Client;

/// <summary>
/// A C# caller of the policy service, written against the contract in
/// dropin/contract and nothing else. It has no idea whether COBOL semantics or
/// the Java 21 mirror is answering, and no setting here says which.
/// </summary>
public sealed class PolicyServiceClient(HttpClient http)
{
    public async Task<HttpResponseMessage> GetPolicyAsync(string policyNumber) =>
        await http.GetAsync($"/api/v1/policies/{policyNumber}");

    public async Task<HttpResponseMessage> GetCoveragesAsync(string policyNumber) =>
        await http.GetAsync($"/api/v1/policies/{policyNumber}/coverages");

    public async Task<HttpResponseMessage> RenewAsync(string policyNumber) =>
        await http.PostAsync($"/api/v1/policies/{policyNumber}/renewals", content: null);

    public async Task<string> GetPolicyXmlAsync(string policyNumber)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, $"/api/v1/policies/{policyNumber}");
        request.Headers.Accept.Add(new System.Net.Http.Headers.MediaTypeWithQualityHeaderValue("application/xml"));
        using var response = await http.SendAsync(request);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadAsStringAsync();
    }

    public static PolicyServiceClient For(string baseUrl) =>
        new(new HttpClient { BaseAddress = new Uri(baseUrl) });
}
