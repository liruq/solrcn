<?php
/**
* 此类将一个xml文件转换为一个数组,此xml的格式必须要是完整封闭的
* 作者 wintion@gmail.com http://www.kandejian.com
* 日期 2007-04-19
*/
class HLXmlToArray
{
	private static function _struct_to_array($values, &$i)
	{
		$child = array();
		if (isset($values[$i]['value'])) array_push($child, $values[$i]['value']);

		while ($i++ < count($values))
		{
			switch ($values[$i]['type'])
			{
				case 'cdata':
					array_push($child, $values[$i]['value']);
					break;

				case 'complete':
					$name = $values[$i]['tag'];
					if(!empty($name))
					{
						$child[$name]= ($values[$i]['value'])?($values[$i]['value']):'';
						if(isset($values[$i]['attributes']))
						{
							$child[$name] = $values[$i]['attributes'];
						}
					}
					break;

				case 'open':
					$name = $values[$i]['tag'];
					$size = isset($child[$name]) ? sizeof($child[$name]) : 0;
					$child[$name][$size] = self::_struct_to_array($values, $i);
					break;

				case 'close':
					return $child;
					break;
			}
		}
		return $child;
	}

   public static function createArray($xml ,$i=0)
   {
        if(empty($xml))
   		{
     		return null;
   		}
   	  // $this->xml = $xml;
       $values = array();
       $index  = array();
       $array  = array();
       $parser = xml_parser_create();
       xml_parser_set_option($parser, XML_OPTION_SKIP_WHITE, 1);
       xml_parser_set_option($parser, XML_OPTION_CASE_FOLDING, 0);
       xml_parse_into_struct($parser, $xml, $values, $index);
       xml_parser_free($parser);
       $name = $values[$i]['tag'];
       $array[$name] = isset($values[$i]['attributes']) ? $values[$i]['attributes'] : '';
       $array[$name] = self::_struct_to_array($values, $i);
       return $array;
   }
}