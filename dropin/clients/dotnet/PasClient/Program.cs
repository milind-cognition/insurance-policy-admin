using Acme.Pas.Client;

// A .NET 8 caller of the policy service.
//
//   dotnet run --project PasClient -- POL-00000042 http://localhost:8081
//
// Point it at the mainframe-backed service or the mirror-backed one. The
// command line is the same and so is the output; the URL is the only thing that
// differs, and in a real cutover even that stays put.

string policyNumber = args.Length > 0 ? args[0] : "PAS-00000042";
string baseUrl = args.Length > 1 ? args[1] : "http://localhost:8081";

var client = PolicyServiceClient.For(baseUrl);

Console.WriteLine($"GET {baseUrl}/api/v1/policies/{policyNumber}");
using var policy = await client.GetPolicyAsync(policyNumber);
Console.WriteLine($"  {(int)policy.StatusCode} {policy.StatusCode}");
Console.WriteLine($"  {await policy.Content.ReadAsStringAsync()}");

Console.WriteLine($"GET {baseUrl}/api/v1/policies/{policyNumber}/coverages");
using var coverages = await client.GetCoveragesAsync(policyNumber);
Console.WriteLine($"  {(int)coverages.StatusCode} {coverages.StatusCode}");
Console.WriteLine($"  {await coverages.Content.ReadAsStringAsync()}");

using var backend = await new HttpClient { BaseAddress = new Uri(baseUrl) }.GetAsync("/manage/backend");
Console.WriteLine($"served by: {await backend.Content.ReadAsStringAsync()}");
