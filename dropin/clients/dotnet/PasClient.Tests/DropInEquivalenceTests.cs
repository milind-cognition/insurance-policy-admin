using System.Net;
using Xunit;

namespace Acme.Pas.Client.Tests;

/// <summary>
/// The ".NET half" of the customer's question, as running code: one C# caller,
/// unchanged, against the mainframe path and the mirror.
/// </summary>
[Collection("both-backends")]
public sealed class DropInEquivalenceTests(BothBackendsFixture backends)
{
    private const string PolicyNumber = "PAS-00000042";

    [Fact]
    public async Task The_policy_document_is_identical_from_both_backends()
    {
        string mainframe = await Body(await backends.Mainframe.GetPolicyAsync(PolicyNumber));
        string mirror = await Body(await backends.Mirror.GetPolicyAsync(PolicyNumber));

        Assert.Contains("\"policyNumber\":\"PAS-00000042\"", mainframe);
        Assert.Equal(mainframe, mirror);
    }

    [Fact]
    public async Task The_coverage_document_is_identical_from_both_backends()
    {
        string mainframe = await Body(await backends.Mainframe.GetCoveragesAsync(PolicyNumber));
        string mirror = await Body(await backends.Mirror.GetCoveragesAsync(PolicyNumber));

        Assert.StartsWith("[{", mainframe);
        Assert.Equal(mainframe, mirror);
    }

    [Fact]
    public async Task The_xml_projection_is_identical_from_both_backends()
    {
        string mainframe = await backends.Mainframe.GetPolicyXmlAsync(PolicyNumber);
        string mirror = await backends.Mirror.GetPolicyXmlAsync(PolicyNumber);

        Assert.StartsWith("<policy>", mainframe);
        Assert.Equal(mainframe, mirror);
    }

    [Fact]
    public async Task An_unknown_policy_is_404_from_both_backends()
    {
        using var mainframe = await backends.Mainframe.GetPolicyAsync("PAS-99999999");
        using var mirror = await backends.Mirror.GetPolicyAsync("PAS-99999999");

        Assert.Equal(HttpStatusCode.NotFound, mainframe.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, mirror.StatusCode);
    }

    private static async Task<string> Body(HttpResponseMessage response)
    {
        using (response)
        {
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }
    }
}
