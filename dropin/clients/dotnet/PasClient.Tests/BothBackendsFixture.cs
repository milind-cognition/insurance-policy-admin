using System.Diagnostics;
using Xunit;

namespace Acme.Pas.Client.Tests;

/// <summary>
/// Starts the policy service twice - once on each backend - so a .NET caller
/// can be pointed at both. Set PAS_MAINFRAME_URL and PAS_MIRROR_URL to use
/// already-running instances instead.
/// </summary>
public sealed class BothBackendsFixture : IDisposable
{
    private readonly List<Process> _started = [];

    public string MainframeUrl { get; }
    public string MirrorUrl { get; }

    public BothBackendsFixture()
    {
        MainframeUrl = Environment.GetEnvironmentVariable("PAS_MAINFRAME_URL")
                       ?? Start("mainframe", 18081);
        MirrorUrl = Environment.GetEnvironmentVariable("PAS_MIRROR_URL")
                    ?? Start("mirror", 18082);
    }

    public PolicyServiceClient Mainframe => PolicyServiceClient.For(MainframeUrl);
    public PolicyServiceClient Mirror => PolicyServiceClient.For(MirrorUrl);

    private string Start(string backend, int port)
    {
        string jar = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory,
            "../../../../../../switch/target/pas-switch-1.0.0.jar"));
        if (!File.Exists(jar))
        {
            throw new InvalidOperationException(
                $"{jar} is missing - run 'make setup' (mvn install) in dropin/ first.");
        }

        var process = Process.Start(new ProcessStartInfo("java",
            $"-jar \"{jar}\" --pas.backend={backend} --server.port={port} " +
            $"--pas.mirror.url=jdbc:h2:mem:dotnet{backend};DB_CLOSE_DELAY=-1;MODE=DB2")
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
        }) ?? throw new InvalidOperationException($"could not start the {backend} service");
        _started.Add(process);

        string url = $"http://localhost:{port}";
        WaitUntilReady(url, backend);
        return url;
    }

    private static void WaitUntilReady(string url, string backend)
    {
        using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(2) };
        for (int attempt = 0; attempt < 60; attempt++)
        {
            try
            {
                string body = http.GetStringAsync($"{url}/manage/backend").Result;
                if (body.Contains(backend))
                {
                    return;
                }
            }
            catch (Exception)
            {
                // not up yet
            }
            Thread.Sleep(1000);
        }
        throw new TimeoutException($"the {backend} service never became ready on {url}");
    }

    public void Dispose()
    {
        foreach (var process in _started)
        {
            try
            {
                process.Kill(entireProcessTree: true);
                process.WaitForExit(10_000);
            }
            catch (Exception)
            {
                // already gone
            }
        }
    }
}

[CollectionDefinition("both-backends")]
public sealed class BothBackendsCollection : ICollectionFixture<BothBackendsFixture>;
