<?php

require_once __DIR__ . '/LibViews.php';
use PHPUnit\Framework\TestCase;

class LibViewsTest extends TestCase
{
    private $instance;

    public function setUp(): void
    {
        $this->instance = new LibViews();
    }

    public function tearDown(): void
    {
        $this->instance = null;
    }

    public function testLoadLib()
    {
        $result1 = $this->instance->loadLib('ui/katalog/plik.css');
        $this->assertEquals('<LINK rel="stylesheet" type="text/css" href="ui/katalog/plik.css">', $result1);

        $result2 = $this->instance->loadLib('ui/katalog/plik.css', '123');
        $this->assertEquals('<LINK rel="stylesheet" type="text/css" href="ui/katalog/plik.css?v=123">', $result2);

        $result3 = $this->instance->loadLib('ui/katalog.plik.js');
        $this->assertEquals('<SCRIPT src="ui/katalog.plik.js">', $result3);

        $result4 = $this->instance->loadLib('ui/katalog.plik.js', '123');
        $this->assertEquals('<SCRIPT src="ui/katalog.plik.js?v=123">', $result4);
    }
}